package ru.grigalgol.example;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This class represents an API client for interacting with CRPT API
 *
 * Used gson for serialization, okhttp as an http client
 */
public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final String JSON_MEDIA_TYPE = "application/json";

    private final Semaphore semaphore;
    private final OkHttpClient httpClient;

    /**
     * Constructs a new CrptApi instance
     *
     * @param timeUnit     the time unit for limiting the requests
     * @param requestLimit the maximum number of requests allowed within the specified time unit
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit);
        httpClient = new OkHttpClient();
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(1));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                semaphore.release(requestLimit - semaphore.availablePermits());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Makes a call to the CRPT API with the specified document and signature
     *
     * @param document the document to be sent to the API
     * @param sign     the signature for the document
     * @return the response from the API
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws IOException          if an I/O error occurs during the API call
     */
    public Response callApi(Document document, String sign) throws InterruptedException, IOException {
        semaphore.acquire();
        Request request = buildRequest(document);
        return httpClient.newCall(request).execute();
    }

    private Request buildRequest(Document document) {
        RequestBody requestBody = RequestBody.create(MediaType.parse(JSON_MEDIA_TYPE), document.toJson());
        return new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build();
    }

    /**
     * Represents a document to be sent to the CRPT API
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Document {
        private String description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private Date productionDate;
        private String productionType;
        private List<Product> products;
        private Date regDate;
        private String regNumber;

        /**
         * Converts the Document object to its JSON representation
         *
         * @return the JSON representation of the Document object
         */
        public String toJson() {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd")
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create();
            return gson.toJson(this);
        }

        /**
         * Represents a product within the document.
         */
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Product {
            private String certificateDocument;
            private Date certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private Date productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);

        for (int i = 0; i < 20; i++) {
            new Thread(() -> {
                try {
                    api.callApi(
                            new Document(
                                    "description",
                                    "docId ",
                                    "docStatus",
                                    "docType",
                                    true,
                                    "ownerInn",
                                    "participantInn",
                                    "producerInn",
                                    new Date(),
                                    "productionType",
                                    List.of(new Document.Product(
                                                    "certificateDocument",
                                                    new Date(),
                                                    "certificateDocumentNumber",
                                                    "ownerInn",
                                                    "producerInn",
                                                    new Date(),
                                                    "tnvedCode",
                                                    "uitCode",
                                                    "uituCode"
                                            )
                                    ),
                                    new Date(),
                                    "some Number"),
                            "someSign");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }
}
