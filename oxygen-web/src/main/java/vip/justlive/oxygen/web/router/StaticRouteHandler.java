/*
 * Copyright (C) 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package vip.justlive.oxygen.web.router;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import vip.justlive.oxygen.core.exception.Exceptions;
import vip.justlive.oxygen.core.io.SimpleResourceLoader;
import vip.justlive.oxygen.core.io.SourceResource;
import vip.justlive.oxygen.core.util.ExpiringMap;
import vip.justlive.oxygen.core.util.ExpiringMap.ExpiringPolicy;
import vip.justlive.oxygen.core.util.FileUtils;
import vip.justlive.oxygen.core.util.HttpHeaders;
import vip.justlive.oxygen.core.util.SnowflakeIdWorker;
import vip.justlive.oxygen.core.util.Strings;
import vip.justlive.oxygen.web.http.Request;
import vip.justlive.oxygen.web.http.Response;

/**
 * 静态资源处理
 *
 * @author wubo
 */
@Slf4j
public class StaticRouteHandler implements RouteHandler {

  private static final File TEMP_DIR;
  private static final Properties MIME_TYPES = new Properties();

  static {
    try {
      MIME_TYPES.load(StaticRouteHandler.class.getResourceAsStream("/mime-types.properties"));
    } catch (IOException e) {
      log.warn("mime types initial failed ", e);
    }
    TEMP_DIR = FileUtils.createTempDir("static");
  }

  private final StaticRoute route;
  private ExpiringMap<String, StaticSource> expiringMap;

  public StaticRouteHandler(StaticRoute route) {
    this.route = route;
    if (this.route.cachingEnabled()) {
      expiringMap = ExpiringMap.<String, StaticSource>builder().name("Static-Source")
          .expiringPolicy(ExpiringPolicy.ACCESSED).expiration(10, TimeUnit.MINUTES)
          .asyncExpiredListeners(this::cleanExpiredFile).build();
    }
  }

  @Override
  public void handle(RoutingContext ctx) {
    StaticSource source = findStaticResource(ctx.requestPath());
    if (source == null) {
      throw Exceptions.fail("not found");
    }
    if (log.isDebugEnabled()) {
      log.debug("handle static source [{}] for path [{}]", source.getPath(), ctx.requestPath());
    }
    Request req = ctx.request();
    Response resp = ctx.response();
    resp.setContentType(source.getContentType());
    String browserETag = req.getHeader(HttpHeaders.IF_NONE_MATCH);
    String ifModifiedSince = req.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
    ZonedDateTime last = Instant.ofEpochMilli(source.lastModified()).atZone(ZoneId.systemDefault());
    String eTag = source.eTag();
    resp.setHeader(HttpHeaders.ETAG, eTag);
    try {
      if (eTag.equals(browserETag) && ifModifiedSince != null && !ZonedDateTime
          .parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME).isBefore(last)) {
        resp.setStatus(304);
        return;
      }
    } catch (DateTimeParseException e) {
      log.warn("Can't parse 'If-Modified-Since' header date [{}]", ifModifiedSince);
    }
    resp.setHeader(HttpHeaders.LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.format(last));
    if (route.maxAge() > 0) {
      resp.setHeader(HttpHeaders.CACHE_CONTROL,
          HttpHeaders.MAX_AGE + Strings.EQUAL + route.maxAge());
    }
    try {
      Files.copy(source.getPath(), resp.getOut());
    } catch (IOException e) {
      throw Exceptions.wrap(e);
    }

  }

  private StaticSource findStaticResource(String path) {
    if (route.cachingEnabled() && expiringMap.containsKey(path)) {
      return expiringMap.get(path);
    }
    StaticSource source = null;
    for (String location : route.locations()) {
      source = findMappedSource(path, location);
      if (source != null) {
        break;
      }
    }
    if (source != null && route.cachingEnabled() && expiringMap.putIfAbsent(path, source) != null) {
      source.remove();
    }
    return source;
  }

  private StaticSource findMappedSource(String path, String basePath) {
    try {
      SourceResource sourceResource = new SimpleResourceLoader(
          basePath + path.substring(route.prefix().length()));
      File file = sourceResource.getFile();
      if (file != null && file.isDirectory()) {
        return null;
      }
      try (InputStream is = sourceResource.getInputStream()) {
        File savedFile = new File(TEMP_DIR, String.valueOf(SnowflakeIdWorker.defaultNextId()));
        Files.copy(is, savedFile.toPath());
        return new StaticSource(savedFile, path);
      }
    } catch (IOException e) {
      // not found or error happens ignore
    }
    return null;
  }

  private void cleanExpiredFile(String key, StaticSource source) {
    if (log.isDebugEnabled()) {
      log.debug("static mapping cached source expired for [{}] [{}]", key, source);
    }
    if (source != null) {
      source.remove();
    }
  }

  /**
   * 静态资源
   */
  @Getter
  static class StaticSource {

    private final Path path;
    private final String contentType;
    private final String requestPath;

    StaticSource(File file, String requestPath) {
      this.path = file.toPath();
      this.requestPath = requestPath;
      String suffix = requestPath.substring(requestPath.lastIndexOf(Strings.DOT) + 1);
      contentType = MIME_TYPES.getProperty(suffix, HttpHeaders.APPLICATION_OCTET_STREAM);
    }

    /**
     * 修改时间
     *
     * @return lastModified
     */
    long lastModified() {
      // 去除毫秒值
      return path.toFile().lastModified() / 1000 * 1000;
    }

    /**
     * etag
     *
     * @return etag
     */
    String eTag() {
      return Strings.DOUBLE_QUOTATION_MARK + lastModified() + Strings.DASH + path.hashCode()
          + Strings.DOUBLE_QUOTATION_MARK;
    }

    void remove() {
      if (path != null) {
        FileUtils.deleteFile(path.toFile());
      }
    }
  }
}
