package org.rapidoid.http;

/*
 * #%L
 * rapidoid-integration-tests
 * %%
 * Copyright (C) 2014 - 2016 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.After;
import org.junit.Before;
import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.commons.Arr;
import org.rapidoid.config.Conf;
import org.rapidoid.crypto.Crypto;
import org.rapidoid.io.IO;
import org.rapidoid.log.Log;
import org.rapidoid.log.LogLevel;
import org.rapidoid.scan.ClasspathUtil;
import org.rapidoid.test.TestCommons;
import org.rapidoid.u.U;
import org.rapidoid.web.On;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public abstract class HttpTestCommons extends TestCommons {

	// FIXME HEAD
	private static final List<String> HTTP_VERBS = U.list("GET", "DELETE", "OPTIONS", "TRACE", "POST", "PUT", "PATCH");

	@Before
	public void openContext() {
		Log.setLogLevel(LogLevel.INFO);

		ClasspathUtil.setRootPackage("some.nonexisting.app");

		System.out.println("--- STARTING SERVER ---");

		Conf.reset();

		On.getDefaultSetup().http().resetConfig();
		On.getDefaultSetup().listen();

		System.out.println("--- SERVER STARTED ---");

		notFound("/");
		notFound("/a");
		notFound("/b?dgfg");
		notFound("/c?x=123");
		notFound("/else");
		notFound("/echo");
		notFound("/upload");
	}

	@After
	public void closeContext() {
		System.out.println("--- STOPPING SERVER ---");

		On.admin().shutdown();
		On.dev().shutdown();

		System.out.println("--- SERVER STOPPED ---");
	}

	protected String localhost(String uri) {
		return localhost(8888, uri);
	}

	protected String localhost(int port, String uri) {
		return "http://localhost:" + port + uri;
	}

	protected void defaultServerSetup() {
		On.get("/echo").plain((Req x) -> {
			return x.verb() + ":" + x.path() + ":" + x.query();
		});

		On.get("/hello").html("Hello");

		On.post("/upload").plain((Req x) -> {
			Log.info("Uploaded files", "files", x.files().keySet());

			return U.join(":", x.cookies().get("foo"), x.cookies().get("COOKIE1"), x.posted().get("a"), x.files()
					.size(), Crypto.md5(x.files().get("f1")), Crypto.md5(x.files().get("f2")), Crypto.md5(U.or(x
					.files().get("f3"), new byte[0])));
		});

		On.req((Req x) -> x.response().html(U.join(":", x.verb(), x.path(), x.query())));
	}

	protected String resourceMD5(String filename) throws IOException, URISyntaxException {
		return Crypto.md5(IO.loadBytes(filename));
	}

	protected String get(String uri) {
		return HTTP.get(localhost(uri)).fetch();
	}

	protected byte[] getBytes(String uri) {
		return HTTP.get(localhost(uri)).execute();
	}

	protected void onlyGet(String uri) {
		onlyGet(8888, uri);
	}

	protected void onlyGet(int port, String uri) {
		onlyReq(port, "GET", uri);
	}

	protected void onlyPost(String uri) {
		onlyPost(8888, uri);
	}

	protected void onlyPost(int port, String uri) {
		onlyReq(port, "POST", uri);
	}

	protected void onlyPut(String uri) {
		onlyPut(8888, uri);
	}

	protected void onlyPut(int port, String uri) {
		onlyReq(port, "PUT", uri);
	}

	protected void onlyDelete(String uri) {
		onlyDelete(8888, uri);
	}

	protected void onlyDelete(int port, String uri) {
		onlyReq(port, "DELETE", uri);
	}

	protected void getAndPost(String uri) {
		getAndPost(8888, uri);
	}

	protected void getAndPost(int port, String uri) {
		testReq(port, "GET", uri);
		testReq(port, "POST", uri);
		notFoundExcept(port, uri, "GET", "POST");
	}

	private void onlyReq(int port, String verb, String uri) {
		testReq(port, verb, uri);
		notFoundExcept(port, uri, verb);
	}

	protected void notFoundExcept(String uri, String... exceptVerbs) {
		notFoundExcept(8888, uri, exceptVerbs);
	}

	protected void notFoundExcept(int port, String uri, String... exceptVerbs) {
		for (String verb : HTTP_VERBS) {
			if (Arr.indexOf(exceptVerbs, verb) < 0) {
				notFound(port, verb, uri);
			}
		}
	}

	protected void notFound(String uri) {
		notFound(8888, uri);
	}

	protected void notFound(int port, String uri) {
		notFoundExcept(port, uri);
	}

	protected void notFound(int port, String verb, String uri) {
		String resp = fetch(port, verb, uri);
		String notFound = IO.load("404-not-found.txt");
		U.notNull(notFound, "404-not-found");
		check(verb + " " + uri, resp, notFound);
	}

	private void testReq(int port, String verb, String uri) {
		String resp = fetch(port, verb, uri);
		String reqName = reqName(port, verb, uri);

		verifyCase(verb + " " + uri, resp, reqName);
	}

	private String fetch(int port, String verb, String uri) {
		byte[] res = HTTP.verb(HttpVerb.from(verb)).url(localhost(port, uri)).raw(true).execute();
		String resp = new String(res);
		resp = resp.replaceFirst("Date: .*? GMT", "Date: XXXXX GMT");

		return resp;
	}

	private String reqName(int port, String verb, String uri) {
		String req = verb + uri.replace("/", "_").replace("?", "-");
		if (port != 8888) {
			req = port + "__" + req;
		}

		return req;
	}

	protected static Map<String, Object> reqResp(Req req, Resp resp) {
		return U.map("verb", req.verb(), "uri", req.uri(), "data", req.data(), "code", resp.code());
	}

}