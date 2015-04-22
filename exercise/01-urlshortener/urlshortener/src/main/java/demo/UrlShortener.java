package demo;

import com.google.common.hash.Hashing;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;

@EnableAutoConfiguration
@ComponentScan
@RestController
public class UrlShortener {
    public static void main(String[] args) {
        SpringApplication.run(UrlShortener.class, args);
    }

    @Value("${urlshorten.url:http://localhost:${server.port:8080}}")
    String urlShortenUrl;

    final ConcurrentHashMap<String, String> urlMap = new ConcurrentHashMap<>(); // key=hash, value=url
    final UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"});

    /**
     * curl -v -X POST http://localhost:8080 -d "url=http://google.com"
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    ResponseEntity<String> save(@RequestParam String url) {
        if (urlValidator.isValid(url)) {
            // FIXED_TODO (1) URLをハッシュ化。ハッシュアルゴリズムには 32-bit murmur3 algorithm を使用する。
            // ヒント: com.google.common.hash.Hashing.murmur3_32()を使う
            String hash;
            try {
                hash = Hashing.murmur3_32().hashBytes(url.getBytes("UTF-8")).toString();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            // FIXED_TODO (2) urlMapにhashに紐づくURLを追加する。
            urlMap.put(hash, url);

            return new ResponseEntity<>(urlShortenUrl + "/" + hash, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * curl -v -X GET http://localhost:8080/58f3ae21
     */
    @RequestMapping(value = "{hash}", method = RequestMethod.GET)
    ResponseEntity<?> get(@PathVariable String hash) {
        String url = urlMap.get(hash);
        // 本当はリダイレクトするのだが、今回はレスポンスボディに入れて200を返す
        if (url != null) {
            return new ResponseEntity<>(url, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
