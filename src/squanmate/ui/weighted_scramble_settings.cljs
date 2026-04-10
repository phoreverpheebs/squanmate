(ns squanmate.ui.weighted-scramble-settings
  (:require [squanmate.ui.common :as common]
            [reagent.core :as reagent]))

(defn default-state []
  (reagent/atom {:weighted-scrambles-enabled? true
                 :show-scramble? true
                 :input-weights "{}"}))

(defn weighted-scramble-options [state]
  [common/form-group
   [common/control-label "Weighted Scrambles"]
   [common/checkbox {:checked (-> @state :weighted-scrambles-enabled?)
                     :on-change #(swap! state update :weighted-scrambles-enabled? not)}
    "Enable weighted scrambles for efficient learning"]
   [common/checkbox {:checked (-> @state :show-scramble?)
                     :on-change #(swap! state update :show-scramble? not)} "Show scramble"]
   [common/input-box (reagent/cursor state [:input-weights]) "Weights"]])
