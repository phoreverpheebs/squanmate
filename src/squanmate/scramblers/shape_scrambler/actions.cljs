(ns squanmate.scramblers.shape-scrambler.actions
  (:require [squanmate.scramblers.shape-scrambler.default-scrambler :as default-scrambler]
            [squanmate.scramblers.shape-scrambler.weighted-scrambler :as weighted-scrambler]
            [squanmate.scramblers.shape-scrambler.predetermined-parity-scrambler :as pps]
            [squanmate.scramblers.shape-scrambler.scrambler :as scrambler]
            [squanmate.services.google-analytics :as ga]
            [squanmate.services.shape-combinations :as shape-combinations]
            [squanmate.services.solving :as solving]
            [clojure.set :as set]
            [squanmate.services.storage :as storage]
            [squanmate.scramblers.shape-scrambler.flip-layers-scrambler :as flip-layers-scrambler]
            [squanmate.ui.inspection-timer :as timer]))

(defonce all-layers (->> shape-combinations/possible-layers
                         (map set)
                         set))

(defn no-cases-selected? [state]
  (let [selected-layers-count (-> @state :selected-shapes count)]
    (<= selected-layers-count 0)))

(defn no-scramble? [state]
  (nil? (:chosen-shapes @state)))

(defn weighted-scrambles-enabled? [state]
  (-> @state :weighted-scramble-settings :weighted-scrambles-enabled?))

(defn select-all-shapes [state]
  (swap! state assoc :selected-shapes all-layers))

(defn select-no-shapes [state]
  (swap! state assoc :selected-shapes #{}))

(defn new-scramble! [state scrambler]
  (let [[chosen-layers new-scramble] (scrambler/create-scramble scrambler)]
    (swap! state assoc
           :scramble-algorithm nil
           :puzzle new-scramble
           :timer (timer/new-count-down-timer 15)
           :chosen-shapes (into #{} chosen-layers)
           :chosen-layers chosen-layers)
    (solving/solve-and-generate-scramble new-scramble state)))

(defn set-new-scramble [state scrambler]
  (new-scramble! state scrambler)
  (ga/send-page-view :trainer/new-scramble))

(defn set-new-weighted-scramble [state]
  (let [s (weighted-scrambler/new-weighted-shape-scrambler (:selected-shapes @state) (:shape-weights @state))]
    (set-new-scramble state s)))

(defn set-new-random-scramble [state]
  (let [s (default-scrambler/new-default-shape-scrambler (:selected-shapes @state))]
    (set-new-scramble state s)))

(defn set-specific-shapes-scramble [state shapes-sets]
  (let [s (default-scrambler/new-default-shape-scrambler shapes-sets)]
    (set-new-scramble state s)))

(defn set-new-repeat-scramble [state]
  (set-specific-shapes-scramble state [(:chosen-shapes @state)]))

(defn set-new-scramble-with-parity [state relative-parity-type]
  (let [s (pps/->PredeterminedParityScrambler (:puzzle @state)
                                              relative-parity-type)]
    (set-new-scramble state s)))

(defn set-new-scramble-with-flipped-layers [state]
  (let [puzzle (:puzzle @state)
        flipper (flip-layers-scrambler/->FlipLayersScrambler puzzle)]
    (set-new-scramble state flipper)))

(defn deselect-case-and-generate-new-scramble! [state]
  (let [this-case (:chosen-shapes @state)]
    (swap! state update :selected-shapes set/difference #{this-case}))
  (set-new-random-scramble state))

(defn start-timer [state]
  ((:start-fn @(:timer @state))))

(defn- inc-total [weight]
  (update weight :total inc))

(defn- inc-correct [weight]
  (-> weight
      (update :correct inc)
      inc-total))

(defn- update-weight-correct [shape-weights shape-case]
  (update shape-weights shape-case #(-> %
                                        (update :correct inc)
                                        inc-total)))

(defn- update-weight-total [shape-weights shape-case]
  (update shape-weights shape-case #(-> %
                                        inc-total)))

(defn mark-shape-correct [state]
  (let [this-case (:chosen-layers @state)
        shape-weights (:shape-weights @state)]
    (swap! state update :shape-weights update-weight-correct this-case)))

(defn mark-shape-incorrect [state]
  (let [this-case (:chosen-layers @state)
        shape-weights (:shape-weights @state)]
    (swap! state update :shape-weights update-weight-total this-case)))

(defn reset-weights [state]
  (print (:shape-weights @state))
  (swap! state update :shape-weights (constantly {}))
  (print "Reset shape weights!"))
