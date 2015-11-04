(ns gyptis.test.core
  (:require [clojure.test :refer :all]
            [gyptis.core :refer :all]
            [gyptis.vega-templates :as vt]))

(deftest title-test
  (testing "adds title-text data and mark"
    (is (= "foobar" (get-in (title vt/top-level "foobar")
                            [:marks 0 :properties :update :text :value]))))
  (testing "only one title-mark across multiple invocations"
    (let [spec (-> vt/top-level
                   (title "foo")
                   (title "bar"))]
      (is (= 1 (-> spec :data count)))
      (is (= 1 (-> spec :marks count)))
      (is (= "bar" (get-in spec
                           [:marks 0 :properties :update :text :value]))))))
