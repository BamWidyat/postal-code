(ns postal-code.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as cs]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def data-out (atom []))

(defn prov-url [prov-name]
  (let [pn (cs/split prov-name #" ")]
    (if (= (count pn) 1)
      prov-name
      (cs/join "%20" pn))))

(def home-url "http://kodepos.nomor.net/_kodepos.php?_i=provinsi-kodepos&sby=000000")

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn make-province-data []
  (drop 19 (map html/text (html/select (fetch-url home-url) [:select :option]))))

(defn url-generator [provno page]
  (if (= page 1)
    (str "http://kodepos.nomor.net/_kodepos.php?_i=desa-kodepos&daerah=Provinsi&jobs="
         (prov-url ((vec (make-province-data)) (dec provno)))
         "&perhal=200&urut=&asc=000101&sby=000000&no1=2&no2=400&kk=0")
     (str "http://kodepos.nomor.net/_kodepos.php?_i=desa-kodepos&daerah=Provinsi&jobs="
          (prov-url ((vec (make-province-data)) (dec provno)))
          "&perhal=200&urut=&asc=000101&sby=000000&no1="
          (-> page
              (- 2)
              (* 200)
              (+ 1))
          "&no2="
          (-> page
              (- 1)
              (* 200))
          "&kk="
          page)))

(defn ktu [url]
 (partition 2 (map html/text (html/select (fetch-url url) [:table :td :a.ktu]))))

(defn ktw-check [data]
  (if (= (first data) "Kode POS")
    data
    (ktw-check (rest data))))

 (defn ktv-fin [x]
   (if (= (count x) 1)
     (apply str x)
     (cs/join " " x)))

(defn ktw [url]
 (partition 3 (ktw-check (map html/text (html/select (fetch-url url) [:table :td :a.ktw])))))

(defn ktv [url]
  (->> (map html/text
           (html/select
            (fetch-url url)
            [:table :td :a.ktv html/first-child]))
      (apply #(cs/split % #" "))
      rest
      ktv-fin))

(defn print-all-province [prov-data]
  (loop [i 1]
    (println (str i ". " ((vec prov-data) (dec i))))
    (if (= i (count prov-data))
      (println "\nTo get all postcode of a province use following command:\n(get-postcode-per-province <province-number>)\n")
      (recur (inc i)))))

(defn get-province-list []
  (->> (make-province-data)
       (print-all-province)))

(defn get-postcode [url]
  (let [ktv (ktv url)
        ktu (ktu url)
        ktw (ktw url)]
    (doseq [line (map (fn [[a b] [x y z]]
                        [ktv z y b a]) ktu ktw)]
      #_(println (cs/join ", " line))
      (swap! data-out conj line))))

(defn write-to-csv [filename]
  (with-open [writer (io/writer (str filename ".csv"))]
    (csv/write-csv writer @data-out)))

(defn getting-postcode [prov]
  (let [prov-name ((vec (make-province-data)) (dec prov))]
    (loop [page 1]
      (println (str "Getting " prov-name " Page " page "..."))
      (get-postcode (url-generator prov page))
      (if (empty? (ktu (url-generator prov (inc page))))
        (println (str "Done getting " prov-name " postcode!"))
        (recur (inc page))))))

(defn get-postcode-per-province [prov]
  (reset! data-out [])
  (if (some #{prov} (range 1 (inc (count (make-province-data)))))
    (do
      (getting-postcode prov)
      (println "Writing data to CSV...")
      (write-to-csv (str ((vec (make-province-data)) (dec prov)) " - " (str (new java.util.Date))))
      (println "DONE!"))
    (println "Input Province Unavailable")))

(defn get-all-postcode []
  (reset! data-out [])
  (let [stop (count (make-province-data))]
    (loop [i 1]
      (getting-postcode i)
      (if (= i stop)
        (do
          (println "Writing data to CSV...")
          (write-to-csv (str "Indonesia - " (str (new java.util.Date))))
          (println "DONE!"))
        (recur (inc i))))))
