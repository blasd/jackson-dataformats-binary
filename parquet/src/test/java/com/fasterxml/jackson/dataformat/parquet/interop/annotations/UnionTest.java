package com.fasterxml.jackson.dataformat.parquet.interop.annotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.UnresolvedUnionException;
import org.apache.avro.reflect.Nullable;
import org.apache.avro.reflect.Union;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.parquet.interop.InteropTestBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for @Union
 */
public class UnionTest extends InteropTestBase {

    @Union({ Cat.class, Dog.class })
    public interface Animal { }

    static class Cat implements Animal {
        @Nullable
        public String color;

        protected Cat() { }
        public Cat(String c) { color = c; }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() == getClass()) {
                Cat other = (Cat) o;
                if (color == null) return other.color == null;
                else return color.equals(other.color);
            }
            return false;
        }
    }

    static class Dog implements Animal {
        public int size;

        protected Dog() { }
        public Dog(int s) { size = s; }

        @Override
        public boolean equals(Object o) {
            return (o.getClass() == getClass())
                && (size == ((Dog) o).size);
        }
    }

    static class Bird implements Animal {
        public boolean flying;

        protected Bird() { }
        public Bird(boolean f) { flying = f; }

        @Override
        public boolean equals(Object o) {
            return (o.getClass() == getClass())
                && (flying == ((Bird) o).flying);
        }
    }

    public static class Cage {
        public Animal animal;

        public Cage(Animal a) { animal = a; }
        protected Cage() { }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Cage) {
                Cage other = (Cage) o;
                if (animal == null) return other.animal == null;
                return animal.equals(other.animal);
            }
            return false;
        }
    }

    public static class PetShop {
        public List<Animal> pets;

        protected PetShop() { }
        public PetShop(Animal... p) {
            pets = Arrays.asList(p);
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() == getClass()) {
                PetShop other = (PetShop) o;
                if (pets == null) return other.pets == null;
                else return pets.equals(other.pets);
            }
            return false;
        }
    }

    @Test
    public void testInterfaceUnionWithCat() throws IOException {
        Cage cage = new Cage(new Cat("test"));
        //
        Cage result = roundTrip(cage);
        //
        assertThat(result).isEqualTo(cage);
    }

    @Test
    public void testInterfaceUnionWithDog() throws IOException {
        Cage cage = new Cage(new Dog(4));
        //
        Cage result = roundTrip(cage);
        //
        assertThat(result).isEqualTo(cage);
    }

    @Test
    public void testInterfaceUnionWithBird() throws IOException {
        Cage cage = new Cage(new Bird(true));
        //
        try {
            roundTrip(cage);
            fail("Should throw exception about Bird not being in union");
        } catch (UnresolvedUnionException | JsonMappingException e) {
            // success
        }
    }

    @Test
    public void testListWithInterfaceUnion() throws IOException {
        PetShop shop = new PetShop(new Cat("tabby"), new Dog(4), new Dog(5), new Cat("calico"));
        //
        PetShop result = roundTrip(shop);
        //
        assertThat(result).isEqualTo(shop);
    }

}
