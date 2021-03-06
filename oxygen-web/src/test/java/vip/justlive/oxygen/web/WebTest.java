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

package vip.justlive.oxygen.web;

import org.junit.Assert;
import org.junit.Test;
import vip.justlive.oxygen.core.net.http.HttpMethod;
import vip.justlive.oxygen.core.net.http.HttpRequest;
import vip.justlive.oxygen.core.net.http.HttpResponse;
import vip.justlive.oxygen.core.util.SystemUtils;
import vip.justlive.oxygen.core.util.ThreadUtils;
import vip.justlive.oxygen.web.router.Router;
import vip.justlive.oxygen.web.server.Server;

/**
 * @author wubo
 */
public class WebTest {

  @Test
  public void test() {

    String msg = "hello world";
    int port = SystemUtils.findAvailablePort();

    Router.router().method(HttpMethod.GET).path("/a").handler(ctx -> ctx.response().write(msg));
    Server server = Server.server();
    new Thread(() -> server.listen(port)).start();

    ThreadUtils.sleep(3000);

    try (HttpResponse response = HttpRequest.get("http://localhost:" + port + "/a").execute()) {
      Assert.assertEquals(msg, response.bodyAsString());
    } catch (Exception e) {
      Assert.fail();
    }

    try (HttpResponse response = HttpRequest.post("http://localhost:" + port + "/b/123")
        .execute()) {
      Assert.assertEquals("123", response.bodyAsString());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }

    server.stop();

  }

}