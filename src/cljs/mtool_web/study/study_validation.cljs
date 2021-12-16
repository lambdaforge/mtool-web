(ns mtool-web.study.study-validation)

(defn check-study [{{:keys [ xStart xEnd xStepSize yStart yEnd yStepSize]} :barChart}]
  (cond
    (zero? xStepSize) "Bar chart X-axis increment has to be larger than zero."
    (zero? yStepSize) "Bar chart Y-axis increment has to be larger than zero."
    (< xEnd xStart) "Bar chart X-axis start has to be smaller than X-axis end."
    (< yEnd yStart) "Bar chart Y-axis start has to be smaller than Y-axis end."
    (pos? (mod (- xEnd xStart) xStepSize)) "Bar chart X-axis increment has to be a dividend of the interval length X-axis start and X-axis end."
    (pos? (mod (- yEnd yStart) yStepSize)) "Bar chart Y-axis increment has to be a dividend of the interval length Y-axis start and Y-axis end."
    :else nil
    ))
