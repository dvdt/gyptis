(ns gyptis.test.util
  (:require [clojure.test :refer :all]
            [gyptis.util :refer :all]))

(deftest test->epoch-millis
  (testing "works with java date types"
    (is (= (->epoch-millis (java.util.Date. 1000)) 1000))
    (is (= (->epoch-millis (clj-time.core/date-time 1970)) 0))))
