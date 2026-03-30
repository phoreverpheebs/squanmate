(ns squanmate.services.color-settings)

(defrecord ColorSettings [top bottom
                          left right
                          front back])

(def defaults (map->ColorSettings
               {:top :black
                :bottom :white
                :left :green
                :right :blue
                :front :red
                :back :orange}))
