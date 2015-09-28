package org.rapidoid.app;

/*
 * #%L
 * rapidoid-app
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
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

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.http.Handler;
import org.rapidoid.http.HttpExchange;
import org.rapidoid.http.HttpExchangeInternals;
import org.rapidoid.io.CustomizableClassLoader;
import org.rapidoid.job.Jobs;

@Authors("Nikolche Mihajlovski")
@Since("2.0.0")
public class AsyncAppHandler implements Handler {

	private final CustomizableClassLoader classLoader;

	private final Handler handler;

	public AsyncAppHandler() {
		this(null);
	}

	public AsyncAppHandler(CustomizableClassLoader classLoader) {
		this.classLoader = classLoader;
		this.handler = new AppHandler();
	}

	@Override
	public Object handle(final HttpExchange x) throws Exception {
		HttpExchangeInternals xi = (HttpExchangeInternals) x;
		xi.setClassLoader(classLoader);

		Jobs.execute(x.asAsyncJob(handler));

		return x;
	}

}
