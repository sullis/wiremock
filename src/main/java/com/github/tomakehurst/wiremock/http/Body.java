/*
 * Copyright (C) 2015-2024 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.http;

import static com.github.tomakehurst.wiremock.common.Encoding.decodeBase64;
import static com.github.tomakehurst.wiremock.common.Encoding.encodeBase64;
import static com.github.tomakehurst.wiremock.common.Strings.stringFromInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.github.tomakehurst.wiremock.common.ContentTypes;
import com.github.tomakehurst.wiremock.common.InputStreamSource;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.StreamSources;
import com.github.tomakehurst.wiremock.common.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.io.IOUtils;


public class Body {

  private final InputStreamSource content;
  private final boolean binary;
  private final boolean json;

  public Body(byte[] content) {
    this(content, true);
  }

  public Body(InputStreamSource stream) {
    this.content = stream;
    this.binary = true;
    this.json = false;
  }

  public Body(String content) {
    this.content = StreamSources.forBytes(Strings.bytesFromString(content));
    binary = false;
    json = false;
  }

  private Body(byte[] content, boolean binary) {
    this(content, binary, false);
  }

  private Body(byte[] content, boolean binary, boolean json) {
    this.content = StreamSources.forBytes(content);
    this.binary = binary;
    this.json = json;
  }

  private Body(JsonNode content) {
    this.content = StreamSources.forBytes(Json.toByteArray(content));
    binary = false;
    json = true;
  }

  static Body fromBytes(byte[] bytes) {
    return bytes != null ? new Body(bytes) : none();
  }

  public static Body fromJsonBytes(byte[] bytes) {
    return bytes != null ? new Body(bytes, false, true) : none();
  }

  static Body fromString(String str) {
    return str != null ? new Body(str) : none();
  }

  public static Body ofBinaryOrText(byte[] content, ContentTypeHeader contentTypeHeader) {
    return new Body(
        content, !ContentTypes.determineIsTextFromMimeType(contentTypeHeader.mimeTypePart()));
  }

  public static Body fromOneOf(byte[] bytes, String str, JsonNode json, String base64) {
    if (bytes != null) return new Body(bytes);
    if (str != null) return new Body(str);
    if (json != null && !(json instanceof NullNode)) return new Body(json);
    if (base64 != null) return new Body(decodeBase64(base64), true);

    return none();
  }

  private static final Body EMPTY_BODY = new Body((byte[]) null);

  public static Body none() {
    return EMPTY_BODY;
  }

  public String asString() {
    return content != null ? stringFromInputStream(content.getStream()) : null;
  }

  public byte[] asBytes() {
    try {
      return content != null ? IOUtils.toByteArray(content.getStream()) : null;
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public InputStream asInputStream() {
    return content != null ? content.getStream() : null;
  }

  public String asBase64() {
    return encodeBase64(asBytes());
  }

  public boolean isBinary() {
    return binary;
  }

  public JsonNode asJson() {
    return Json.node(asString());
  }

  public boolean isJson() {
    return json;
  }

  public boolean isAbsent() {
    return content == null;
  }

  public boolean isPresent() {
    return !isAbsent();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Body body = (Body) o;
    return Objects.equals(binary, body.binary) && Arrays.equals(asBytes(), body.asBytes());
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(asBytes()), binary);
  }

  @Override
  public String toString() {
    return "Body {" + "content=" + asString() + ", binary=" + binary + ", json=" + json + '}';
  }
}
