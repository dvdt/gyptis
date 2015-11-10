(ns gyptis.test.validate
  "Functions for validating generated vega JSON against the vega jsonschema.
  Inspired by https://github.com/myfreeweb/octohipster/blob/master/src/octohipster/validator.clj"
  (:require [clojure.data.json :as json])
  (:import [com.github.fge.jsonschema.core.exceptions.ProcessingException]
           [com.github.fge.jsonschema.core.report.ProcessingReport]
           [com.github.fge.jsonschema.main JsonSchema JsonValidator JsonSchemaFactory]
           [com.fasterxml.jackson.core JsonFactory]
           [com.github.fge.jackson JsonLoader]

           [com.fasterxml.jackson.databind JsonNode ObjectMapper]))

(defonce ^JsonNode vega-schema
  (JsonLoader/fromString (slurp "resources/vega-schema.json")))

(defn ^JsonNode clj->jsonnode [x]
  (JsonLoader/fromString (json/write-str x)))

(defn validate
  [vega-spec]
  (.. (JsonSchemaFactory/byDefault)
      (getValidator)
      (validate vega-schema (clj->jsonnode vega-spec))))

(defn valid?
  [vega-spec]
  (.isSuccess (validate vega-spec)))
