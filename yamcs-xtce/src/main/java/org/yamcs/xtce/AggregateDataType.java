package org.yamcs.xtce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.yamcs.protobuf.Yamcs.Value;
import org.yamcs.protobuf.Yamcs.Value.Type;
import org.yamcs.xtce.util.AggregateMemberNames;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class AggregateDataType extends NameDescription implements DataType {
    private static final long serialVersionUID = 1L;

    List<Member> memberList = new ArrayList<>();
    transient AggregateMemberNames memberNames;

    public AggregateDataType(Builder<?> builder) {
        super(builder);

        this.memberList = builder.memberList;

    }

    public AggregateDataType(String name) {
        super(name);
    }

    protected AggregateDataType(AggregateDataType t) {
        super(t);
        this.memberList = t.memberList;
        this.memberNames = t.memberNames;
    }

    @Override
    public String getTypeAsString() {
        return "aggregate";
    }

    /**
     * Returns a member on the given name. If no such member is present return null
     * 
     * @param name
     *            the name of the member to be returned
     * @return the member with the given name
     */
    public Member getMember(String name) {
        for (Member m : memberList) {
            if (name.equals(m.getName())) {
                return m;
            }
        }
        return null;
    }

    public List<Member> getMemberList() {
        return memberList;
    }

    @Override
    public Type getValueType() {
        return Value.Type.AGGREGATE;
    }

    /**
     * Returns a member in a hierarchical aggregate. It is equivalent with a chained call of {@link #getMember(String)}:
     * 
     * <pre>
     * getMember(path[0]).getMember(path[1])...getMember(path[n])
     * </pre>
     * 
     * assuming that all the elements on the path exist.
     * 
     * 
     * @param path
     *            - the path to be traversed. Its length has to be at least 1 - otherwise an
     *            {@link IllegalArgumentException} will be thrown.
     * @return the member obtained by traversing the path or null if not such member exist.
     */
    public Member getMember(String[] path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("path cannot be empty");
        }
        Member m = getMember(path[0]);
        for (int i = 1; i < path.length; i++) {
            if (m == null) {
                return null;
            }
            DataType ptype = m.getType();
            if (ptype instanceof AggregateParameterType) {
                m = ((AggregateParameterType) ptype).getMember(path[i]);
            } else {
                return null;
            }
            m = getMember(path[i]);
        }
        return m;
    }

    /**
     * 
     * @return the (unique) object encoding the member names
     * 
     */
    public AggregateMemberNames getMemberNames() {
        if (memberNames == null) {
            String[] n = memberList.stream().map(m -> m.getName()).toArray(String[]::new);
            memberNames = AggregateMemberNames.get(n);
        }
        return memberNames;
    }

    public int numMembers() {
        return memberList.size();
    }

    public Member getMember(int idx) {
        return memberList.get(idx);
    }

    /**
     * Parse the initial value as a JSON string.
     * <p>
     * This allows to specify only partially the values, the rest are copied from the member initial value or
     * the type definition (an exception is thrown if there is any member for which the value cannot be determined).
     * 
     * 
     * @param initialValue
     * @return a map containing the values for all members.
     * @throws IllegalArgumentException
     *             if the string cannot be parsed or if values cannot be determined for all members
     * 
     */
    public Map<String, Object> parseString(String initialValue) {
        // parse it as json
        try {
            JsonElement je = new JsonParser().parse(initialValue);
            if (je instanceof JsonObject) {
                return fromJson((JsonObject) je);
            } else {
                throw new IllegalArgumentException("Expected JSON object but found " + je.getClass());
            }
        } catch (JsonParseException jpe) {
            throw new IllegalArgumentException(jpe.toString());
        }
    }

    private Map<String, Object> fromJson(JsonObject jobj) {
        Map<String, Object> r = new HashMap<>();
        for (Member memb : memberList) {
            if (jobj.has(memb.getName())) {
                JsonElement jsel = jobj.remove(memb.getName());
                String v;
                if (jsel.isJsonPrimitive() && jsel.getAsJsonPrimitive().isString()) {
                    v = jsel.getAsString();
                } else {
                    v = jsel.toString();
                }
                r.put(memb.getName(), memb.getType().parseString(v));
            } else {
                Object v = memb.getInitialValue();
                if (v == null) {
                    v = memb.getType().getInitialValue();
                }
                if (v == null) {
                    throw new IllegalArgumentException("No value could be determined for member '"
                            + memb.getName() + "' (its corresponding type does not have an initial value)");
                }
                r.put(memb.getName(), v);
            }
        }
        if (jobj.size() > 0) {
            throw new IllegalArgumentException("Unknown members "
                    + jobj.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList()));
        }
        return r;
    }

    @Override
    public Object parseStringForRawValue(String stringValue) {
        // parse it as json
        try {
            JsonElement je = new JsonParser().parse(stringValue);
            if (je instanceof JsonObject) {
                return fromJsonRaw((JsonObject) je);
            } else {
                throw new IllegalArgumentException("Expected JSON object but found " + je.getClass());
            }
        } catch (JsonParseException jpe) {
            throw new IllegalArgumentException(jpe.toString());
        }
    }

    private Map<String, Object> fromJsonRaw(JsonObject jobj) {
        Map<String, Object> r = new HashMap<>();
        for (Member memb : memberList) {
            if (jobj.has(memb.getName())) {
                JsonElement jsel = jobj.remove(memb.getName());
                String v;
                if (jsel.isJsonPrimitive() && jsel.getAsJsonPrimitive().isString()) {
                    v = jsel.getAsString();
                } else {
                    v = jsel.toString();
                }
                r.put(memb.getName(), memb.getType().parseStringForRawValue(v));
            } else {
                throw new IllegalArgumentException("No value for member '" + memb.getName()+"'");
            }
        }
        if (jobj.size() > 0) {
            throw new IllegalArgumentException("Unknown members "
                    + jobj.entrySet().stream().map(e -> e.getKey()).collect(Collectors.toList()));
        }
        return r;
    }

    @Override
    public Map<String, Object> getInitialValue() {
        Map<String, Object> r = new HashMap<String, Object>();
        for (Member memb : memberList) {
            Object v = memb.getInitialValue();
            if (v == null) {
                DataType dt = memb.getType();
                if (dt != null) {
                    v = dt.getInitialValue();
                }
            }
            if (v == null) {
                return null;
            }
            r.put(memb.getName(), v);
        }
        return r;
    }

    @Override
    public String toString(Object v) {
        if (v instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m1 = (Map<String, Object>) v;
            Map<String, Object> m2 = new HashMap<>();

            for (Member memb : memberList) {
                Object v2 = m1.get(memb.getName());
                if (v != null) {
                    DataType dt = memb.getType();
                    m2.put(memb.getName(), dt.toString(v2));
                }
            }
            Gson gson = new Gson();
            return gson.toJson(m2);
        } else {
            throw new IllegalArgumentException("Can only convert maps");
        }
    }

    public abstract static class Builder<T extends Builder<T>> extends NameDescription.Builder<T>
            implements DataType.Builder<T> {
        List<Member> memberList = new ArrayList<>();

        public Builder() {
        }

        public Builder(AggregateDataType dataType) {
            super(dataType);
            this.memberList = dataType.memberList;
        }

        @Override
        public T setInitialValue(String initialValue) {
            throw new UnsupportedOperationException(
                    "Cannot set initial value; please send individual initial values for the members");

        }

        public T addMember(Member memberType) {
            memberList.add(memberType);
            return self();
        }

        public T addMembers(List<Member> memberList) {
            this.memberList.addAll(memberList);
            return self();
        }

        public List<Member> getMemberList() {
            return memberList;
        }

        public boolean isResolved() {
            return true;
        }
    }

}
