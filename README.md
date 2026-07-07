# PageWeaver Java SDK

Official Java client for the [PageWeaver](https://pageweaver.io) PDF generation API. Uses the JDK's built-in HTTP client; Gson for JSON. Java 11+.

## Install

```xml
<dependency>
  <groupId>io.pageweaver</groupId>
  <artifactId>pageweaver</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Usage

```java
import io.pageweaver.PageWeaver;
import java.time.Duration;
import java.util.Map;

PageWeaver pw = new PageWeaver("pk_live_...");

// Create a document and wait for it to finish rendering
Map<String, Object> doc = pw.createAndWait(
    Map.of("templateId", "tmpl_invoice",
           "payload", Map.of("number", "INV-001", "total", 4200)),
    Duration.ofSeconds(1),
    Duration.ofSeconds(60));
System.out.println(doc.get("status")); // "done"

// Or fire-and-poll yourself
Map<String, Object> created = pw.createDocument(Map.of("html", "<h1>Hello</h1>"));
Map<String, Object> result = pw.getDocument((String) created.get("id"));
```

Non-2xx responses throw `PageWeaverException` (with `getStatusCode()` and `getBody()`).

## Releasing

Publishing to Maven Central requires a Sonatype Central account and GPG signing keys, so no release workflow is wired yet. `mvn -B verify` builds and tests locally / in CI.

## License

MIT
