/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.textuality.keybase.lib;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * wrapper
 */
public interface KeybaseUrlConnectionClient {

    Response getUrlResponse(URL url, Proxy proxy, boolean isKeybase) throws IOException;
    String getKeybaseBaseUrl();

    class Response {
        private final InputStream stream;
        private final int code;
        private final String message;
        private final Map<String, List<String>> headers;

        public Response(InputStream stream, int code, String  message, Map<String, List<String>> headers) {
            if (stream == null) {
                throw new IllegalArgumentException("Stream may not be null.");
            }
            this.stream = stream;
            this.code = code;
            this.message = message;
            this.headers = headers;
        }

        public InputStream getStream() {
            return stream;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }
    }
}
