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
    (is (valid? (facet-global (stacked-bar data))))
    (is (valid? (dodged-bar data)))
    (is (valid? (facet-global (dodged-bar data))))
    (is (valid? (point data)))
    (is (valid? (facet-global (point data))))
    (is (valid? (line data {:stroke :fill})))
    (is (valid? (facet-global (line data {:stroke :fill}))))))

(deftest facet-test
  (testing "Facetting causes positioned stacked-bar scale to adjust accordingly."
    (let [facet-spec (facet-global (stacked-bar [{:x 1 :y "a"}]) {:facet_x :facet_xx})
          stack-position (get-in facet-spec [:data 1])]
      (is (= "stats" (:name stack-position)))
      (is (= [:x :facet_xx :facet_y]
             (get-in stack-position [:transform 0 :groupby]))))))

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
