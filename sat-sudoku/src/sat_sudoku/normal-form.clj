;;; # SAT Partie 2 : manipulations de formules propositionnelles

;;; Ne pas oublier la dépendance suivante dans project.clj
;;; [org.clojure/core.match "0.3.0-alpha5"]

(ns sat-partie2
  (:require [clojure.core.match :refer [match]])
  (:require [clojure.set :as set]))


;; EXERCICE : ajouter  l'implication et l'équivalence dans tout ce qui suit

'(==> a b)
'(<=> a b)


;;; ## Simplifications des formules

(defn simplify-one [f]
  (match f
    ;; *** simplification du not ***
    ;; (not true) -> false
    (['not true] :seq) false
    ;; (not false) -> true
    (['not false] :seq) true
    ;; (not (not a)) -> a
    (['not (['not a] :seq)] :seq) a
    ;; *** simplification du or ***
    ;; (or true a) -> true
    (['or true a] :seq) true
    ;; (or a true) -> true
    (['or a true] :seq) true
    ;; (or false a) -> a
    (['or false a] :seq) a
    ;; (or a false) -> a
    (['or a false] :seq) a
    ;; *** simplification du and ***
    ;; (and true a) -> a
    (['and true a] :seq) a
    ;; (and a true) -> a
    (['and a true] :seq) a
    ;; (and false a) -> false
    (['and false a] :seq) false
    ;; (and a false) -> false
    (['and a false] :seq) false
    ;; *** simplification de l'implication ==> ***
    ;;(==> true true) -> true
    (['==> true true] :seq) true
    ;;(==> true false) -> false
    (['==> true false] :seq) false
    ;;(==> false a) -> true
    (['==> false a] :seq) true
    ;; *** simplification de l'équivalence <=> ***
    ;;(<=> true true) -> true
    (['<=> true true] :seq) true
    ;;(<=> true false) -> false
    (['<=> true false] :seq) false
    ;;(<=> false true) -> false
    (['<=> false true] :seq) false
    ;;(<=> false false) -> true
    (['<=> false false] :seq) true
    :else f))

(simplify-one '(not true))
(simplify-one '(not false))
(simplify-one '(not (not true)))
(simplify-one '(not (or true (not false))))

(defn simplify [f]
  (match f
    ([op a] :seq) (simplify-one (list op (simplify a)))
    ([op a b] :seq) (simplify-one (list op (simplify a)
                                          (simplify b)))
    :else f))

(simplify '(or (not (or (not true)
                        (and (or (not x) false)
                             (or (not false) x))))
               (not (or y (and false z)))))

(simplify '(not (or true (not false))))

(simplify '(==> (or true (not false)) false))

(simplify '(<=> (not (or true (not false))) false))

;;; ## Forme normale NNF

(defn nnf' [f]
  (match f
    ;; not .. and
    (['not (['and a b] :seq)] :seq)
    (list 'or (nnf' (list 'not  a)) (nnf' (list 'not b)))
    ;; not .. or
    (['not (['or a b] :seq)] :seq)
    (list 'and (nnf' (list 'not  a)) (nnf' (list 'not b)))
    ;: not .. not
    (['not (['not a] :seq)] :seq) (nnf' a)
    ;; and ..
    (['and a b] :seq) (list 'and (nnf' a) (nnf' b))
    ;; or ..
    (['or a b] :seq) (list 'or (nnf' a) (nnf' b))
    ;; TODO ==> et <=>
    ;; not ==> a b  -> (and (nnf a) (nnf (not b)))
    (['not (['==> a b] :seq)] :seq) (list 'and (nnf a) (nnf (list 'not b)))
    ;; ==> a b  -> (or (nnf (not a)) (nnf b))
    (['==> a b] :seq) (list 'or (nnf (list 'not a)) (nnf b))
    ;; not <=> a b -> (or (and (nnf (not a)) (nnf b)) (and (nnf a) (nnf (not b))))
    (['not (['<=> a b] :seq)] :seq)
         (list 'or (list 'and (nnf (list 'not a)) (nnf b)) (list 'and (nnf a) (nnf (list 'not b))))
    ;; <=> a b -> (and (or (nnf (not a)) (nnf b)) (or (nnf a) (nnf (not b))))
    (['<=> a b] :seq)
         (list 'and (list 'or (nnf (list 'not a)) (nnf b)) (list 'or (nnf a) (nnf (list 'not b))))
    :else f))

(nnf' '(not (or true (not false))))
(nnf' '(or (not (or (not true)
                    (and (or (not x) false)
                         (or (not false) x))))
           (not (or y (and false z)))))

(nnf' '(==> (or a b) (<=> c d)))

(simplify '(or (not (or (not true)
                    (and (or (not x) false)
                         (or (not false) x))))
           (not (or y (and false z)))))

(simplify (nnf' '(or (not (or (not true)
                              (and (or (not x) false)
                                   (or (not false) x))))
                     (not (or y (and false z))))))




(defn nnf [f]
  (nnf' (simplify f)))

;;; ## Forme normale disjonctive DNF

(defn distrib [f]
  (match f
    (['and (['or a b] :seq) c] :seq)
    (list 'or (distrib (list 'and a c))
              (distrib (list 'and b c)))
    (['and a (['or b c] :seq)] :seq)
    (list 'or (distrib (list 'and a b))
              (distrib (list 'and a c)))
    :else f))

;; Remarque : f doit être en NNF
(defn dnf' [f]
  (match f
    (['and a b] :seq) (distrib (list 'and (dnf' a)
                                          (dnf' b)))
    (['or a b] :seq) (list 'or (dnf' a)
                               (dnf' b))
    :else f))

(defn dnf [f]
  (dnf' (nnf f)))

(dnf '(and (or a (and b c)) (or (not a) (not c))))

(and true true true false)

;;; Problème : c'est pas lisible et c'est simplifiable
;;; Solution : représentation sous forme d'ensemble (conjonctif) de clauses (disjonctives)

(defn setify-and [f]
  (match f
    (['and a b] :seq)
    (set/union (setify-and a) (setify-and b))
    :else #{f}))

(setify-and '(and a (and a (and (not b ) (not b)))))

(defn setify-dnf [f]
  (match f
   (['and a b] :seq) #{(setify-and f)}
   (['or a b] :seq) (set/union (setify-dnf a) (setify-dnf b))
   :else #{#{f}}))

(setify-dnf
  '(or (or (and a (not a)) (and a (not c))) (or (and (and b c) (not a)) (and (and b c) (not c)))))


;; EXERCICE : retirer les clauses qui contiennent un litéral et sa négation
;; fonction :  filter-trivial



(defn filter-trivial-aux [f]
  (match f
     (['and a (['not b] :seq)] :seq) (if (= a b)
                                       ()
                                       f)
     ([op a] :seq) (list op (filter-trivial-aux a))
     ([op a b] :seq)
         (let [filter-a (filter-trivial-aux a)
               filter-b (filter-trivial-aux b)]
           (if (= () filter-a)
             (if (= () filter-b)
               ()
               (list op filter-b))
             (if (= () filter-b)
               (list op filter-a)
               (list op filter-a filter-b))))
     :else f))

(defn filter-trivial [f]
  (let [res (filter-trivial-aux f)]
    (if (= res ())
      false
      (match res
         (['or a] :seq) (dnf a)
         :else res))))

(filter-trivial '(or (and a (not a)) (or (and a (not b)) a)))

;; EXERCICE : si on a une clause C1 incluse dans une clause C2
;; (par exemple: #{a (not b)}   et  #{a (not b) c})
;; alors on retire la plus grande C2 ..
;; fonction :  filter-subsume



(defn filter-subsume-aux [setf]
  (loop [setf setf, res setf, i (dec (count setf))]
    (if (zero? i)
      res
      (



(defn filter-subsume [f]
  (let [setf (setify-dnf f)]
    ))



;; EXERCICE : en déduire une fonction dnfs qui prend une
;; formule quelconque et retourne la formule DNF simplifiée représentée par des ensembles

;; EXERCICE :  comment passer d'une DNF sous forme d'ensemble d'ensembles à une CNF ?
;;             (indice : la CNF d'une formule f  est liée à la DNF de (not f) )

;; En déduire une fonction :  cnfs prend une
;; formule quelconque et retourne la formule CNF simplifiée représentée par des ensembles
;; (en passant par la représentation DNF)
