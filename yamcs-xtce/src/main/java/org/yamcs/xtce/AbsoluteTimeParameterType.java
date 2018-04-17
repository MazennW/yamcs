package org.yamcs.xtce;


import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AbsoluteTimeParameterType extends AbsoluteTimeDataType implements ParameterType {
    private static final long serialVersionUID = 1L;

    public AbsoluteTimeParameterType(String name){
        super(name);
    }

    /**
     * Creates a shallow copy of the parameter type, giving it a new name. 
     */
    public AbsoluteTimeParameterType(AbsoluteTimeParameterType t) {
        super(t);
    }

  
    @Override
    public String getTypeAsString() {
        return "integer";
    }

    @Override
    public List<UnitType> getUnitSet() {
        return Collections.emptyList();
    }

    @Override
    public Set<Parameter> getDependentParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasAlarm() {
        return false;
    }
    @Override
    public String toString() {
        return "AbsoluteTimeParameterType name:"+name
                +((getReferenceTime()!=null)?", referenceTime:"+getReferenceTime():"");
               
    }

   
}
