(ns snake_game.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :refer [register-handler register-sub subscribe dispatch dispatch-sync]]
            [goog.events :as events]))

(enable-console-print!)

(def board [15 10])

(defn rand-free-position
  "This function takes the snake and the board-size as arguments, and returns a random position not colliding with the snake body"
  [snake [x y]]
  (let [snake-positions-set (into #{} (:body snake))
        board-positions (for [x-pos (range x)
                              y-pos (range y)]
                          [x-pos y-pos])]
    (when-let [free-positions (seq (remove snake-positions-set board-positions))]
      (rand-nth free-positions))))

(def snake {:direction [1 0]
            :body [[3 2] [2 2] [1 2] [0 2]]})


(def initial-state {:board board
                    :snake snake
                    :point (rand-free-position snake, board)
                    :points 0
                    :game-running? true
                    :direction-changed false
                    :stored-direction false})


; Move snake
(defn move-snake
  "Move the whole snake based on positions and directions of each snake body segments"
  [{:keys [direction body] :as snake}]
  (let [head-new-position (mapv + direction (first body))] ;applies + to direction and body meaning 1(direction) + 1(body)
    (update-in snake [:body] #(into [] (drop-last (cons head-new-position body))))))

    ;;Dispatch the next state event every 150ms
(defonce snake-moving
  (js/setInterval #(dispatch [:next-state]) 150))

(def key-code->move
  "Mapping from the integer key code to the direction vector corresponding to that key"
  {38 [0 -1]
    40 [0 1]
    39 [1 0]
    37 [-1 0]})

(defn change-snake-direction
  "Changes the snake head direction, only when it's perpendicular to the old head direction"
  [[new-x new-y] [x y]]
  (if (or (= x new-x)
          (= y new-y))
    [x y]
    [new-x new-y]))

(defonce key-handler
  (events/listen js/window "keydown"
                  (fn [e]
                    (let [key-code (.-keyCode e)]
                      (when (contains? key-code->move key-code)
                        (dispatch [:change-direction (key-code->move key-code)]))))))

(register-handler
  :change-direction
  (fn [db [_ new-direction]]
    (update-in db [:snake :direction]
                (partial change-snake-direction new-direction))))

(register-handler
  :initialize
  (fn
    [db _]
    (merge db initial-state)))

(register-handler
  :next-state
  (fn
    [db _]
    (if (:game-running? db)
        (update db :snake move-snake)
      db)))

(register-sub
  :board
  (fn
    [db _]                         ;; db is the app-db atom 
    (reaction (:board @db))))      ;; pulls the board

(register-sub
  :snake
  (fn
    [db _]
    (reaction (:body (:snake @db)))))
             
(register-sub
  :point
  (fn
    [db _]
    (reaction (:point @db))))

(register-sub
  :points
  (fn
    [db _]
    (reaction (:points @db))))

(register-sub
  :game-running?
  (fn
    [db _]
    (reaction (:game-running? @db))))

(defn render-board
  "Renders the game board area with snake and the food item"
  []
  (let [board (subscribe [:board])
        snake (subscribe [:snake])
        point (subscribe [:point])]
    (fn []
      (let [[width height] @board
            snake-positions (into #{} @snake)
            current-point @point
            cells (for [y (range height)]
                    (into [:tr]
                          (for [x (range width)
                                :let [current-pos [x y]]]
                            (cond
                              (snake-positions current-pos) [:td.snake-on-cell]
                              (= current-pos current-point) [:td.point]
                              :else [:td.cell]))))]
        (into [:table.stage {:style {:height 377
                                      :width 527}}]
              cells)))))

(defn score
  "Renders player's score"
  []
  (let [points (subscribe [:points])]
    (fn []
      [:div.score (str "Score: " @points)])))
      
(defn game-over
  "Renders the game over overlay if the game is finished"
  []
  (let [game-state (subscribe [:game-running?])]
    (fn []
      (if @game-state
        [:div]
        [:div.overlay
          [:div.play 
          [:h1 "↺" ]]]))))

(defn game
  "The main rendering function"
  []
  [:div
  [render-board]
  [score]
  [game-over]
  ])
  

(defn run
  "The main app function"
  []
  (dispatch-sync [:initialize])
  (reagent/render [game]
                  (js/document.getElementById "app")))

(run)