(ns squanmate.scramblers.shape-scrambler.weighted-scrambler
  (:require [clojure.set :as set]
            [squanmate.alg.puzzle :as p]
            [squanmate.alg.rotation :as rotation]
            [squanmate.scramblers.shape-scrambler.scrambler :as scrambler]
            [squanmate.scramblers.shape-scrambler.default-scrambler :as d]))

(defn- success-rate [weight]
  (/ (:correct weight) (:total weight)))

(defn- all-with-inverted [possible-layers]
  (let [layers (map into (repeat []) possible-layers)]
    (concat layers (map reverse layers))))

(defn- compute-success-rates [weights]
  (into (sorted-map-by (fn [k1 k2] (< (success-rate (get weights k1)) (success-rate (get weights k2))))) weights))

(defn- choose-top-and-bottom-shape-names [possible-layers weights]
  (->> weights
       compute-success-rates
       keys
       (take (max 2 (/ (count weights) 6)))
       (concat (set/difference (set (all-with-inverted possible-layers)) (set (keys weights))))
       rand-nth
       cycle))

(defn new-weighted-shape-scrambler
  "Generates scrambles guaranteed to match the desired shapes, but weighted for shapes that
  need more practice over others."
  ([possible-layers weights]
   (print weights)
   (reify scrambler/ShapeScrambler
     (create-scramble [this]
       (let [[top-name bottom-name] (choose-top-and-bottom-shape-names possible-layers weights)
             top (d/shape-str top-name)
             bottom (d/shape-str bottom-name)
             scrambled-puzzle (-> (p/puzzle-with-shapes top bottom) d/apply-random-rotations)]
         (println (get weights [top-name bottom-name]))
         (println "Scramble" top-name "/" bottom-name "selected with weight" (success-rate (get weights [top-name bottom-name])))
         [[top-name bottom-name] scrambled-puzzle])))))
