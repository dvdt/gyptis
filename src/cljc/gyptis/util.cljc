(ns gyptis.util
  #?(:clj
     (:require [clojure.data.json :as json])))

(defn merge-spec
  "Similar to how leiningen merges profiles:
  keys are assoc'd, vectors are appended, sets are unioned.
  :replace metadata on b causes the contents of b to completely override a."
  ([a b]
   (let [result (cond
                  (nil? a) b
                  (nil? b) a
                  (:replace (meta b)) b
                  (map? a) (merge-with merge-spec a b)
                  (vector? a) (vec (concat a b))
                  :else b)]
     (if (:replace (meta a))
       ^:replace result
       result)))
  ([a b & more]
   (apply merge-with merge-spec a b more)))

(defn ->json
  "Converts clojure datastructure to json"
  [x]
  #?(:clj
     (json/write-str x)

     :cljs
     (.stringify js/JSON x)))
