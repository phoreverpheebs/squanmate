(ns squanmate.ui.weighted-scramble-settings
  (:require [squanmate.ui.common :as common]
            [reagent.core :as reagent]))

(defn default-state []
  (reagent/atom {:weighted-scrambles-enabled? false :shape-weights {}}))

(defn weighted-scramble-options [state]
  [common/form-group
   [common/control-label "Weighted Scrambles"]
   [common/checkbox {:checked (-> @state :weighted-scrambles-enabled?)
                     :on-change #(swap! state update :weighted-scrambles-enabled? not)}
    "Enable weighted scrambles for efficient learning"]])
