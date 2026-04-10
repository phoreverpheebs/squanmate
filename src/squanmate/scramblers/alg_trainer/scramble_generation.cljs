(ns squanmate.scramblers.alg-trainer.scramble-generation
  (:require [squanmate.scramblers.alg-trainer.algset-scrambler :as algset-scrambler]
            [squanmate.services.google-analytics :as ga]
            [squanmate.services.solving :as solving]
            [squanmate.scramblers.algsets.orient-both-layers :as obl]
            [squanmate.scramblers.algsets.edge-permutation :as ep]
            [squanmate.scramblers.algsets.permute-last-layer :as pll]
            [squanmate.scramblers.algsets.lin-corner-permutation :as lin-cp]
            [squanmate.scramblers.algsets.lin-pll-plus-1 :as lin-pll-plus-1]
            [squanmate.scramblers.algsets.cubeshape :as cubeshape]
            [squanmate.scramblers.shape-scrambler.weighted-scrambler :as weighted-scrambler]))

(defn- set-scramble-for-start-position! [state puzzle]
  (swap! state assoc :puzzle puzzle)
  (solving/solve-and-generate-scramble puzzle state))

(defn- random-case [state]
  (let [cases (:selected-cases @state)
        case (rand-nth (seq cases))]
    case))

(defn- select-weighted-case [state]
  (let [cases (:selected-cases @state)
        weights (:case-weights @state)
        case (weighted-scrambler/select-cases cases weights)]
    case))

(defn- report-error-for-case [alg e]
  (js/console.log "Error setting up case for alg: " alg ". Description: " e))

(def algsets
  "The algorithm sets the algorithm trainer supports"
  [{:name "Cubeshape"
    :algset cubeshape/cubeshape-algset}
   {:name "Orient both layers (OBL)"
    :algset obl/obl-algset}
   {:name "Edge permutation (EP)"
    :algset ep/ep-algset}
   {:name "Permute last layer (PLL)"
    :algset pll/pll-algset}
   {:name "Lin corner permutation"
    :algset lin-cp/lin-cp-algset}
   {:name "Lin PLL+1"
    :algset lin-pll-plus-1/lin-pll-plus-1-algset}])

(defn- algset-for-case [case]
  (first (filter (fn [algset]
                   (let [all-cases (algset-scrambler/all-cases algset)]
                     (some (partial = case) all-cases)))
                 (map :algset algsets))))

(defn new-scramble
  ([state]
   (new-scramble state (random-case state)))
  ([state case]
   (let [algset (algset-for-case case)]
     (assert algset (str "Internal error: algset not found for case " (pr-str case)))
     (let [puzzle (algset-scrambler/generate-puzzle algset case)]
       (swap! state assoc :scramble-algorithm ""
              :chosen-case case)
       (set-scramble-for-start-position! state puzzle)))))

(defn set-new-weighted-scramble! [state]
  ([state]
   (new-scramble state (select-weighted-case state))
   (ga/send-page-view :algorithm-trainer/new-scramble)))

(defn set-new-scramble! [& args]
  (apply new-scramble args)
  (ga/send-page-view :algorithm-trainer/new-scramble))

(defn- inc-total [weight]
  (update weight :total inc))

(defn- inc-correct [weight]
  (-> weight
      (update :correct inc)
      inc-total))

(defn- update-weight-correct [case-weights this-case]
  (update case-weights this-case #(-> %
                                      (update :correct inc)
                                      inc-total)))

(defn- update-weight-total [case-weights this-case]
  (update case-weights this-case inc-total))

(defn write-weights [state]
  (let [weights (pr-str (:case-weights @state))]
    (swap! state update-in [:weighted-scramble-settings :input-weights] (constantly weights))))

(defn mark-case-correct [state]
  (when-some [this-case (:chosen-case @state)]
    (let [case-weights (:case-weights @state)]
      (swap! state update :case-weights update-weight-correct this-case))
    (write-weights state)))

(defn mark-case-incorrect [state]
  (when-some [this-case (:chosen-case @state)]
    (let [case-weights (:case-weights @state)]
      (swap! state update :case-weights update-weight-total this-case))
    (write-weights state)))

(defn mark-correct-and-set-new-weighted-scramble! [state]
  (mark-case-correct state)
  (set-new-weighted-scramble! state))

(defn mark-incorrect-and-set-new-weighted-scramble! [state]
  (mark-case-incorrect state)
  (set-new-weighted-scramble! state))
