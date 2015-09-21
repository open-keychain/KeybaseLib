/*
 * Copyright (C) 2014 Tim Bray <tbray@textuality.com>
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

import com.textuality.keybase.lib.prover.Fetch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class KeybaseQuery {

    public static final int REDIRECT_TRIES = 5;

    private KeybaseUrlConnectionClient connectionClient;
    private Proxy proxy;

    public KeybaseQuery(KeybaseUrlConnectionClient connectionClient) {
        this.connectionClient = connectionClient;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Iterable<Match> search(String query) throws KeybaseException {
        JSONObject result = getFromKeybase("_/api/1.0/user/autocomplete.json?q=", query);
        try {
            return new MatchIterator(JWalk.getArray(result, "completions"));
        } catch (JSONException e) {
            throw KeybaseException.keybaseScrewup(e);
        }
    }

    public JSONObject getFromKeybase(String path, String query) throws KeybaseException {
        try {
            String url = "https://keybase.io/" + path + URLEncoder.encode(query, "utf8");

            URL realUrl = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) connectionClient.openConnection(realUrl, proxy);
            conn.connect();

            int response = conn.getResponseCode();

            if (response >= 200 && response < 300) {
                String text = snarf(conn.getInputStream());
                try {
                    JSONObject json = new JSONObject(text);
                    if (JWalk.getInt(json, "status", "code") != 0) {
                        throw KeybaseException.queryScrewup("Keybase.io query failed: " + path + "?" + query +
                                " using proxy: " + proxy);
                    }
                    return json;
                } catch (JSONException e) {
                    throw KeybaseException.keybaseScrewup(e);
                }
            } else {
                String message = snarf(conn.getErrorStream());
                throw KeybaseException.networkScrewup("Keybase.io query error (status=" + response + "): " + message);
            }
        } catch (Exception e) {
            throw KeybaseException.networkScrewup(e);
        }
    }

    public static String snarf(InputStream in)
            throws IOException {
        byte[] buf = new byte[1024];
        int count = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        while ((count = in.read(buf)) != -1)
            out.write(buf, 0, count);
        return out.toString();
    }

    public Fetch fetchProof(String urlString) {
        Fetch result = new Fetch();

        try {
            HttpURLConnection conn = null;
            int status = 0;
            int redirects = 0;
            while (redirects < REDIRECT_TRIES) {
                result.mActualUrl = urlString;
                URL url = new URL(urlString);
                conn = (HttpURLConnection) connectionClient.openConnection(url, proxy);
                conn.addRequestProperty("User-Agent", "Keybase Java client, github.com/timbray/KeybaseLib");
                conn.connect();
                status = conn.getResponseCode();
                if (status == 301) {
                    redirects++;
                    urlString = conn.getHeaderFields().get("Location").get(0);
                } else {
                    break;
                }
            }
            if (status >= 200 && status < 300) {
                result.mBody = KeybaseQuery.snarf(conn.getInputStream());
            } else {
                result.mProblem = "Fetch failed, status " + status + ": " + KeybaseQuery.snarf(conn.getErrorStream());
            }

        } catch (MalformedURLException e) {
            result.mProblem = "Bad URL: " + urlString;
        } catch (IOException e) {
            result.mProblem = "Network error: " + e.getLocalizedMessage();
        }

        return result;
    }

    static class MatchIterator implements Iterable<Match>, Iterator<Match> {

        private final JSONArray mMatches;
        private int mLastIndex;
        private Match mNextMatch;

        public MatchIterator(JSONArray matches) throws KeybaseException {
            mMatches = matches;
            mLastIndex = -1;
            mNextMatch = null;
            hasNext();
        }

        // caches mNextMatch but not the index
        private int findNext() {
            try {
                for (int index = mLastIndex + 1; index < mMatches.length(); index++) {
                    mNextMatch = new Match(mMatches.getJSONObject(index));
                    if (mNextMatch.hasKey()) {
                        return index;
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(KeybaseException.keybaseScrewup(e));
            } catch (KeybaseException e) {
                throw new RuntimeException(e);
            }
           return -1;
        }

        @Override
        public boolean hasNext() {
            return (findNext() != -1);
        }

        @Override
        public Match next() {
            mLastIndex = findNext();
            if (mLastIndex == -1) {
                throw new NoSuchElementException();
            } else {
                return mNextMatch;
            }
        }

        @Override
        public void remove() {
            throw new RuntimeException("UserIterator.remove() not supported");
        }

        @Override
        public Iterator<Match> iterator() {
            return this;
        }
    }
}
