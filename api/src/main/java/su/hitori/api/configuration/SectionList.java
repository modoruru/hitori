package su.hitori.api.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class SectionList<Scheme extends SectionScheme> extends ArrayList<Scheme> {

    private final Field<List<SectionScheme>> field;

    private SectionList(Field<List<SectionScheme>> field, Collection<? extends Scheme> collection) {
        super(collection);
        this.field = field;

        for (int i = 0; i < this.size(); i++) {
            this.get(i).setInTheList(i, field);
        }
    }

    @Override
    public Scheme set(int index, Scheme element) {
        Scheme previous = super.set(index, element);
        previous.setInTheList(-1, null);
        element.setInTheList(index, field);
        return previous;
    }

    @Override
    public boolean add(Scheme scheme) {
        if(!super.add(scheme)) return false;
        scheme.setInTheList(size() - 1, field);
        return true;
    }

    @Override
    public void add(int index, Scheme element) {
        int beforeAddSize = size();
        super.add(index, element);
        if(index <= beforeAddSize) {
            for (int i = index; i < beforeAddSize + 1; i++) {
                get(i).setInTheList(i, field);
            }
        }
    }

}
