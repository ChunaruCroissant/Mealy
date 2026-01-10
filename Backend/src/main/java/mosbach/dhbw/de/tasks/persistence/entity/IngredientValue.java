package mosbach.dhbw.de.tasks.persistence.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class IngredientValue {

    private String name;
    private String unit;
    private String amount; // bleibt erstmal String wie in IngredientConv

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
