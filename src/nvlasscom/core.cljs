(ns nvlasscom.core
  #_(:require [clojure.browser.repl :as repl]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

(println "The particles have evolved. The previous particles cannot be recovered.")

(def ctx
  (.getContext (.getElementById js/document "particles-evolved")
               "2d"))

(def jitter-prob 0.03)
(def emit-prob 0.075)
;; (def tail-len 5)
(def box-edge 4)

;;; FIXME: read from element
(def screen-width 800)
(def screen-width-2 400)

(def screen-height 600)
(def screen-height-2 300)



(def p-state
  (atom {:particles []}))

(def direction-map
  {0 {:ux 1 :uy 0}
   1 {:ux -1 :uy 0}
   2 {:ux 0 :uy 1}
   3 {:ux 0 :uy -1}})

(defn make-particle
  ([x y jitter-prob dominant-direction]
   (merge {:x x :y y :tail '()
           :tail-len (rand-nth [4 5])
           :dir dominant-direction
           :jitter-prob jitter-prob}
          (get direction-map dominant-direction)))
  ([dominant-direction]
   (make-particle 0 0 jitter-prob dominant-direction)))

(defn cut-tail [{:keys [tail tail-len] :as p}]
  (if (> (count tail) tail-len)
    (update p :tail #(take tail-len tail))
    p))

(defn move-particle [{:keys [x y] :as p}]
  (-> p
      (update :tail conj {:x x :y y})
      cut-tail
      (update :x + (:ux p))
      (update :y + (:uy p))))

(def jmap {0 -1 1 1})

(defn jitter [{:keys [dir] :as p}]
  (let [rand-jump (get jmap (rand-int 2))]
    (if (#{0 1} dir)
      (update p :y + rand-jump)
      (update p :x + rand-jump))))


(def color-map
  ["#11FF11"
   "#00AA00"
   "#008800"
   "#003300"
   "#002200"
   "#000000"])


;;; plot with offsets and stuff -- 0,0 is center of screen,
;;; 
;;; fixed dims for now

(defn transl-coords [{:keys [x y] :as p}]
  (assoc p
         :plot-x (+ (* x box-edge) screen-width-2)
         :plot-y (+ (* y box-edge) screen-height-2)))

(defn plot-box [{:keys [plot-x plot-y] :as b} color]
  (set! (.-fillStyle ctx) color)
  (.fillRect ctx
             (- plot-x (/ box-edge 2)) (- plot-y (/ box-edge 2))
             box-edge box-edge))
             
  
(defn plot-particle [p]
  ;; plot current
  (let [c0    (first color-map)
        crest (rest color-map)]
    (plot-box (transl-coords p) c0)
    ;; plot rest with an ugly approach
    (doseq [[pi ci] (map (fn [x y] [x y])
                         (:tail p)
                         crest)]
      (plot-box (transl-coords pi) ci))))

;;; set Interval, no requestAnimationFrame yet

(defn maybe-new-particle [state]
  (if (< (rand) emit-prob)
    (let [new-dir (rand-int 4)]
      (update state :particles conj (make-particle new-dir)))
    state))

(defn rep-jitter [dir]
  (if (or (= dir 0) (= dir 1)) (rand-nth [2 3])
      (rand-nth [0 1])))
        

(defn full-move-particle [p]
  (if (< (rand) (:jitter-prob p))
    [(-> p move-particle jitter)
     (make-particle (:x p) (:y p) (/ (:jitter-prob p) 2)
                    (rep-jitter (:dir p)))]
    [(move-particle p)]))

(defn update-state [state]
  (-> state
      maybe-new-particle
      (update :particles #(mapcat full-move-particle %))))


(defn paint-state [state]
  (doseq [p (:particles state)]
    (plot-particle p))
    ;; Fixed particle out of state
  (plot-box (transl-coords {:x 0 :y 0}) "#FF2222")
  state)

(defn remove-dead [ps]
  (filter (fn [{:keys [x y] :as p}]
            ;; fixme: calc from w, h
            (and (> x -103) (< x 103)
                 (> y -78) (< y 78)))
          ps))

(def interv
  (.setInterval
   js/window (fn []
               (reset! p-state
                       (-> @p-state
                           update-state
                           (update :particles remove-dead)
                           paint-state)))
   100))

;; (plot-box (transl-coords {:x 10 :y 10}) "#00FF00")
;; (plot-box (transl-coords {:x 0 :y 0}) "#FFAA00")




