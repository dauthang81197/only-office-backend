package com.example.onlyoffice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Payload the Document Server POSTs to the callback URL.
 * See the "status" field for the document lifecycle:
 * 1 = editing, 2 = ready to save (MustSave), 3 = save error,
 * 4 = closed no changes, 6 = force-save, 7 = force-save error.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackRequest {

    private int status;

    /** URL on the Document Server to download the edited document (status 2 and 6). */
    private String url;

    /** Document version key. */
    private String key;

    /** JWT token when the config used a secret; validated instead of the header. */
    private String token;

    private List<String> users;

    private List<Action> actions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Action {
        /** 0 = user disconnected, 1 = user connected, 2 = user force-save request. */
        private int type;
        private String userid;
    }
}
