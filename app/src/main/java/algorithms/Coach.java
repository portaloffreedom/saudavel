package algorithms;

/**
 * Created by lenin on 15/10/15.
 */
public class Coach {
    private final String GENDER_MALE = "M";
    private final double CONST_BMR_MALE_FACTOR = 88.362;
    private final double CONST_BMR_MALE_WEIGHT_PONDEROSITY = 13.397;
    private final double CONST_BMR_MALE_HEIGHT_PONDEROSITY = 4.799;
    private final double CONST_BMR_MALE_AGE_PONDEROSITY = 5.677;
    private final double CONST_BMR_FEMALE_FACTOR = 447.593;
    private final double CONST_BMR_FEMALE_WEIGHT_PONDEROSITY = 9.247;
    private final double CONST_BMR_FEMALE_HEIGHT_PONDEROSITY = 3.098;
    private final double CONST_BMR_FEMALE_AGE_PONDEROSITY = 4.330;
    private final double CONST_ES_TARGET_FACTOR = 9300;
    private final int CONST_CALORIC_DEFICIT = 500;


    public double calculateBMR (String gender, double weight, double height, int age) {
        double result = 0.0;
        // It should check if the gender is only equals to M or F (and would be nice to throw an exception if not)...
        // Future work :P
        if (gender.equals(GENDER_MALE)) {
            result = CONST_BMR_MALE_FACTOR + (weight * CONST_BMR_MALE_WEIGHT_PONDEROSITY) +
                    (height * CONST_BMR_MALE_HEIGHT_PONDEROSITY) -
                    (age * CONST_BMR_MALE_AGE_PONDEROSITY);
        } else {
            result = CONST_BMR_FEMALE_FACTOR + (weight * CONST_BMR_FEMALE_WEIGHT_PONDEROSITY) +
                    (height * CONST_BMR_FEMALE_HEIGHT_PONDEROSITY) -
                    (age * CONST_BMR_FEMALE_AGE_PONDEROSITY);
        }
        return result;
    }

    public double calculateESTarget (double deltaWeight, int daysToGoal) {
        return (deltaWeight * CONST_ES_TARGET_FACTOR) / daysToGoal;
    }

    public double calculateWeighChangeTarget (int daysToGoal) {
        return (daysToGoal * CONST_CALORIC_DEFICIT) / CONST_ES_TARGET_FACTOR;
    }
}
