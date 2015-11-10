(ns gyptis.test.core
  (:require [clojure.test :refer :all]
            [gyptis.core :refer :all]
            [gyptis.vega-templates :as vt]
            [gyptis.test.validate :refer [valid? validate]]))

(deftest schema-test
  (let [data [{:x "n=2", :y 1 :fill "n-1"}
              {:x "n=2", :y 0 :fill "n-2"}
              {:x "n=3", :y 1 :fill "n-1"}
              {:x "n=3", :y 1 :fill "n-2"}
              {:x "n=4", :y 2 :fill "n-1"}
              {:x "n=4", :y 1 :fill "n-2"}
              {:x "n=5", :y 3 :fill "n-1"}
              {:x "n=5", :y 2 :fill "n-2"}
              {:x "n=6", :y 5 :fill "n-1"}
              {:x "n=6", :y 3 :fill "n-2"}]]
    (is (valid? (stacked-bar data)))
    (is (valid? (dodged-bar data)))
    (is (valid? (point data)))
    (is (valid? (line (map #(assoc % :stroke (:fill %)) data))))))

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
