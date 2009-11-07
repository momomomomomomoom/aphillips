/*
 * @(#)ClassUtils.java     9 Feb 2009
 * 
 * Copyright © 2009 Andrew Phillips.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.qrmedia.commons.lang;

import static com.qrmedia.commons.validation.ValidationUtils.checkNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for dealing with @{link Class Classes}.
 * 
 * @author anph
 * @since 9 Feb 2009
 *
 */
public class ClassUtils {

    /**
     * Returns <u>one</u> of the possible chains of superclasses and/or interfaces joining the
     * specified class to the given superclass, as returned by {@link #getSuperclassChains(Class, Class)}.
     * <i>Which</i> of the possible chains will be returned is not defined.
     * <p>
     * If <code>superclass</code> is <i>not</i> a superclass or -interface of <code>class</code>,
     * the method returns <code>null</code>. This may happen (in spite of the signature) if the 
     * method is called with non-generic arguments.
     *
     * @param <T>       the type of the class at the &quot;start&quot; of the superclass chain
     * @param clazz     the class at the &quot;start&quot; of the superclass chain
     * @param superclass        the class at the &quot;end&quot; of the superclass chain
     * @return <u>one</u> superclass chain linking <code>class</code> to <code>superclass</code>,
     *         where successive elements of the list are immediate superclasses or -interfaces. If
     *         <code>class</code> is not a subclass of <code>superclass</code>, returns <code>null</code>.
     * @throws IllegalArgumentException if either argument is null    
     * @see #getSuperclassChains(Class, Class)    
     */
    public static <U, V extends U> List<Class<? super V>> getSuperclassChain(Class<V> clazz, 
            Class<U> superclass) {
        Set<List<Class<? super V>>> superclassChains = getSuperclassChainsInternal(clazz, superclass, true);
        return ((superclassChains != null) ? superclassChains.iterator().next() : null);
    }
    
    /**
     * Returns the chain of superclass and/or interfaces from the specified class to the given
     * superclass. Either parameter may be an interface.
     * <p>
     * Each list in the resulting set contains immediate superclass elements in order, i.e. for
     * classes
     * 
     * <pre>
     * class Foo {}
     * class Bar extends Foo {}
     * class Baz extends Bar {}
     * </pre>
     * 
     * <code>getSuperclassChains(Baz.class, Foo.class)</code> will return one list, <code>[Baz.class,
     * Bar.class, Foo.class]</code>.
     * <p>
     * If both parameters are classes, there can only be one possible chain. However, if the superclass
     * is an interface, there may be multiple possible inheritance chains. For instance, for
     * 
     * <pre>
     * interface Foo {}
     * interface Bar1 extends Foo {}
     * interface Bar2 extends Foo {}
     * interface Baz extends Bar1, Bar2 {}
     * </pre>
     * 
     * both <code>[Baz.class, Bar1.class, Foo.class]</code> and <code>[Baz.class, Bar2.class, Foo.class]</code>
     * are valid inheritance chains, and the method returns both.
     * <p>
     * If <code>superclass</code> is <i>not</i> a superclass or -interface of <code>class</code>,
     * the method returns <code>null</code>. This may happen (in spite of the signature) if the 
     * method is called with non-generic arguments.
     *
     * @param <T>       the type of the class at the &quot;start&quot; of the superclass chain 
     * @param clazz     the class at the &quot;start&quot; of the superclass chain
     * @param superclass        the class at the &quot;end&quot; of the superclass chain
     * @return all possible superclass chains linking <code>class</code> to <code>superclass</code>,
     *         where successive elements of the list are immediate superclasses or -interfaces. If
     *         <code>class</code> is not a subclass of <code>superclass</code>, returns <code>null</code>.
     * @throws IllegalArgumentException if either argument is null  
     * @see #getSuperclassChain(Class, Class)      
     */
    public static <T> Set<List<Class<? super T>>> getSuperclassChains(Class<T> clazz, Class<? super T> superclass) {
        return getSuperclassChainsInternal(clazz, superclass, false);
    }
    
    private static <U, V extends U> Set<List<Class<? super V>>> getSuperclassChainsInternal(Class<V> clazz, 
            Class<U> superclass, boolean oneChainSufficient) {
        checkNotNull("'clazz' and 'superclass' may not be non-null", clazz, superclass);
        
        if (!superclass.isAssignableFrom(clazz)) {
            return null;
        }
        
        // interfaces only need to be considered if the superclass is an interface
        Set<List<Class<? super V>>> superclassChains = ClassUtils.<U, V>getSuperclassSubchains(
                    clazz, superclass, oneChainSufficient, superclass.isInterface());
        
        // should return null if no chains are found
        return (!superclassChains.isEmpty() ? superclassChains : null);
    }
    
    // recursive method: gets the subchains from the given class to the target class
    @SuppressWarnings("unchecked")
    private static <U, V extends U> Set<List<Class<? super V>>> getSuperclassSubchains(
            Class<? super V> subclass, Class<U> superclass, boolean oneChainSufficient, 
            boolean considerInterfaces) {
        
        // base case: the subclass *is* the target class
        if (subclass.equals(superclass)) {
            
            // since the list will be built from the *head*, a linked list is a good choice
            List<Class<? super V>> subchain = new LinkedList<Class<? super V>>();
            subchain.add(subclass);
            return Collections.singleton(subchain);
        }
        
        // recursive case: get all superclasses and, if required, interfaces and recurse
        Set<Class<? super V>> supertypes = new HashSet<Class<? super V>>();
        
        Class<? super V> immediateSuperclass = subclass.getSuperclass();
        
        // interfaces and Object don't have a superclass
        if (immediateSuperclass != null) {
            supertypes.add(immediateSuperclass);
        }
        
        if (considerInterfaces) {
            supertypes.addAll(Arrays.asList((Class<? super V>[]) subclass.getInterfaces()));
        }
        
        Set<List<Class<? super V>>> subchains = new HashSet<List<Class<? super V>>>();
        
        for (Class<? super V> supertype : supertypes) {
            
            // the compiler complains if the type parameters to getSuperclassSubchains aren't specified 
            Set<List<Class<? super V>>> subchainsFromSupertype = 
                ClassUtils.<U, V>getSuperclassSubchains(supertype, superclass, 
                        oneChainSufficient, considerInterfaces);
            
            // each chain from the supertype results in a chain [current, subchain-from-super]
            if (!subchainsFromSupertype.isEmpty()) {
                
                if (oneChainSufficient) {
                    ClassUtils.<V>addSubchain(subchains, subclass, 
                            subchainsFromSupertype.iterator().next());
                    return subchains;
                } else {
                    
                    for (List<Class<? super V>> subchainFromSupertype : subchainsFromSupertype) {
                        ClassUtils.<V>addSubchain(subchains, subclass, subchainFromSupertype);
                    }
                    
                }
                
            }
            
        }
        
        return subchains;
    }

    // adds the class to the beginning of the subchain and stores this extended subchain
    private static <T> void addSubchain(Set<List<Class<? super T>>> subchains, 
            Class<? super T> clazz, List<Class<? super T>> subchainFromSupertype) {
        subchainFromSupertype.add(0, clazz);
        subchains.add(subchainFromSupertype);
    }

    /**
     * Determines if <i>any</i> of the given superclasses is a superclass or -interface of the
     * specified class. See {@link Class#isAssignableFrom(Class)}.
     * 
     * @param superclasses      the superclasses and -interfaces to be tested against
     * @param clazz     the class to be to be checked
     * @return  <code>true</code> iff <i>any</i> of the given classes is a superclass or -interface
     *          of the specified class
     * @throws IllegalArgumentException if either of the arguments is null
     */
    public static boolean isAnyAssignableFrom(Collection<Class<?>> superclasses,
            Class<?> clazz) {
        checkNotNull("All arguments must be non-null", superclasses, clazz);

        for (Class<?> superclass : superclasses) {
            
            if (superclass.isAssignableFrom(clazz)) {
                return true;
            }
            
        }
        
        return false;
    }
    
    /**
     * Collects all fields declared in the given class <u>and</u> inherited from 
     * parents.
     * 
     * @param clazz the class to parse
     * @return  a list of all fields declared in the class or its  parents, in the 
     *          order determined by successive {@link Class#getDeclaredFields()}
     *          calls
     * @see #getAllDeclaredFields(Class, Class)          
     */
    public static List<Field> getAllDeclaredFields(Class<?> clazz) {
        return getAllDeclaredFields(clazz, Object.class);
    }

    /**
     * Collects all fields declared in the given class <u>and</u> inherited from 
     * parents <strong>up to and including the given parent class</strong>.
     * 
     * @param <T> the type of the class to parse
     * @param clazz the class to parse
     * @param superclass the superclass of the class to parse at which traversal should be
     *                   stopped
     * @return  a list of all fields declared in the class or its parents up to and including
     *          the given parent class, in the order determined by successive 
     *          {@link Class#getDeclaredFields()} calls
     * @see #getAllDeclaredFields(Class)         
     */
    public static <T> List<Field> getAllDeclaredFields(Class<T> clazz, 
            Class<? super T> superclass) {
        final List<Field> fields = new ArrayList<Field>();
        
        for (Class<?> immediateSuperclass : getSuperclassChain(clazz, superclass)) {
            fields.addAll(Arrays.asList(immediateSuperclass.getDeclaredFields()));
        }
        
        return fields;
    }
    
    /**
     * Collects all fields declared in the given class <u>and</u> inherited from 
     * parents that are annotated with an annotation of the given type.
     * 
     * @param clazz the class to parse
     * @param annotationType the non-{@code null} type (class) of the annotation required
     * @return  a list of all fields declared in the class or its  parents, in the 
     *          order determined by successive {@link Class#getDeclaredFields()}
     *          calls
     * @throws IllegalArgumentException if {@code clazz} or {@code annotationType} is {@code null}          
     */
    public static List<Field> getAllAnnotatedDeclaredFields(Class<?> clazz, 
            Class<? extends Annotation> annotationType) {
        checkNotNull("All arguments must be non-null", clazz, annotationType);
        
        final List<Field> annotatedFields = new ArrayList<Field>();
        
        for (Field field : getAllDeclaredFields(clazz)) {
            
            if (field.isAnnotationPresent(annotationType)) {
                annotatedFields.add(field);
            }
            
        }
        
        return annotatedFields;
    }
    
    /**
     * Collects all methods of the given class (as returned by {@link Class#getMethods()} that 
     * are annotated with the given annotation.
     * 
     * @param clazz     the class whose methods should be returned
     * @param annotationType        the annotation that the returned methods should be annotated with
     * @return  the methods of the given class annotated with the given annotation
     * @throws IllegalArgumentException if {@code clazz} is {@code null}
     */
    public static Set<Method> getAnnotatedMethods(Class<?> clazz, 
            Class<? extends Annotation> annotationType) {
        checkNotNull("'clazz' must be non-null", clazz);
        
        // perhaps this case should throw an exception, but an empty list also seems sensible 
        if (annotationType == null) {
            return new HashSet<Method>();
        }
        
        Set<Method> annotatedMethods = new HashSet<Method>();
        
        for (Method method : clazz.getMethods()) {
            
            if (method.isAnnotationPresent(annotationType)) {
                annotatedMethods.add(method);
            }
            
        }
        
        return annotatedMethods;
    }
    
    /**
     * Retrieves the type arguments of a class when regarded as an subclass of the
     * given typed superclass or interface. The order of the runtime type classes matches the order
     * of the type variables in the declaration of the typed superclass or interface.
     * <p>
     * For example, for the classes
     * 
     * <pre>
     * class Foo&lt;U, V&gt; {}
     * class Bar&lt;W&gt; extends Foo&lt;String, W&gt; {}
     * class Baz extends Bar&lt;Long&gt;
     * </pre>
     * 
     * and a <code>typedClass</code> argument of <code>Baz.class</code>, the method should return
     * <p>
     * <ul>
     * <li><code>[String, Long]</code> for a <code>typedSuperclass</code> argument of <code>Foo.class</code>,
     *     and
     * <li><code>[Long]</code> if <code>typedSuperclass</code> is <code>Bar.class</code>.
     * </ul>
     * For type parameters that cannot be determined, <code>null</code> is returned.
     * <p>
     * <b>Note:</b> It is <u>not</u> possible to retrieve type information that is not available
     * in the (super)class hierarchy at <u>compile</u>-time. Calling 
     * <code>getActualTypeArguments(new ArrayList&lt;String&gt;().getClass(), List.class)</code> will, 
     * for instance, return <code>[null]</code> because the specification of the actual type 
     * (<code>String</code>, in this example) did not take place either in the superclass {@link AbstractList} 
     * or the interface {@link List}.
     * 
     * @param <U>       the type of the object
     * @param typedClass the class for which type information is required
     * @param typedSuperclass the typed class or interface of which the object is to be regarded a 
     *                        subclass
     * @return  the type arguments for the given class when regarded as a subclass of the
     *          given typed superclass, in the order defined in the superclass
     * @throws IllegalArgumentException if <code>typedSuperclass</code> or <code>typedClass</code> 
     *                                  is <code>null</code>         
     */
    public static <U> List<Class<?>> getActualTypeArguments(Class<U> typedClass,
            Class<? super U> typedSuperclass) {
        checkNotNull("All arguments must be non-null", typedSuperclass, typedClass);
        
        // the type signature should ensure that the class really *is* an subclass of typedSuperclass
        assert (typedSuperclass.isAssignableFrom(typedClass)) 
        : Arrays.<Class<?>>asList(typedSuperclass, typedClass);

        TypeVariable<?>[] typedClassTypeParams = typedSuperclass.getTypeParameters();
        
        // if the class has no parameters, return
        if (typedClassTypeParams.length == 0) {
            return new ArrayList<Class<?>>(0);
        }
        
        /*
         * It would be nice if the parent class simply "aggregated" all the type variable
         * assignments that happen in subclasses. In other words, it would be nice if, in the
         * example in the JavaDoc, new Baz().getClass().getSuperclass().getGenericSuperclass()
         * would return [String, Long] as actual type arguments.
         * Unfortunately, though, it returns [String, W], because the assignment of Long to W
         * isn't accessible to Bar. W's value is available from new Baz().getClass().getGenericSuperclass(),
         * and must be "remembered" as we traverse the object hierarchy.
         * Note, though, that the "variable substitution" of W (the variable used in Bar) for V (the
         * equivalent variable in Foo) *is* propagated, but only to the immediate parent!
         */
        Map<TypeVariable<?>, Class<?>> typeAssignments = 
            new HashMap<TypeVariable<?>, Class<?>>(typedClassTypeParams.length);
        
        /*
         * Get one possible path from the typed class to the typed superclass. For classes, there
         * is only one (the superclass chain), but for interfaces there may be multiple. We only
         * need one, however (and it doesn't matter which one) since the compiler does not allow
         * inheritance chains with conflicting generic type information.
         */
        List<Class<? super U>> superclassChain = getSuperclassChain(typedClass, typedSuperclass);
        
        assert (superclassChain != null) : Arrays.<Class<?>>asList(typedSuperclass, typedClass);
        
        /*
         * The list is ordered so that successive elements are immediate superclasses. The iteration
         * stops with the class whose *superclass* is the last element, because type information
         * is collected from the superclass.
         */
        for (int i = 0; i < superclassChain.size() - 1; i++) {
            collectAssignments(superclassChain.get(i), superclassChain.get(i + 1), typeAssignments);
        }
        
        // will contain null for entries for which no class could be resolved
        return getActualAssignments(typedClassTypeParams, typeAssignments);
    }

    private static void collectAssignments(Class<?> clazz, Class<?> supertype,
            Map<TypeVariable<?>, Class<?>> typeAssignments) {
        TypeVariable<?>[] typeParameters = supertype.getTypeParameters();
        
        // the superclass is not necessarily a generic class
        if (typeParameters.length == 0) {
            return;
        }
        
        Type[] actualTypeArguments = getGenericSupertype(clazz, supertype).getActualTypeArguments();

        assert (typeParameters.length == actualTypeArguments.length) 
        : Arrays.asList(typeParameters, typeAssignments);
        
        // matches up type parameters with their actual assignments, assuming the order is the same!
        for (int i = 0; i < actualTypeArguments.length; i++) {
            Type type = actualTypeArguments[i];
            
            // type will be a Class if the actual type is known, and a TypeVariable if not
            if (type instanceof Class) {
                typeAssignments.put(typeParameters[i], (Class<?>) type);
            } else {
                assert (type instanceof TypeVariable<?>) : type;
                
                /*
                 * The actual type arguments consist of classes and type variables from the
                 * immediate child class. So if the type assignment mapping is updated to
                 * contain the mapping of all type variables of the *current* class to
                 * their classes, then these can be used in the *next* iteration to resolve
                 * any variable "left over" from this round.
                 * 
                 * Any variables that cannot be resolved in this round are not resolvable, otherwise
                 * the would have been resolved in the previous round.
                 */
                if (typeAssignments.containsKey(type)) {
                    typeAssignments.put(typeParameters[i], typeAssignments.get(type));
                }
                
            }
            
        }

    }
    
    private static ParameterizedType getGenericSupertype(Class<?> clazz, Class<?> supertype) {
        
        if (!supertype.isInterface()) {
            return (ParameterizedType) clazz.getGenericSuperclass();
        } else {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            
            for (int i = 0; i < genericInterfaces.length; i++) {
                Type interfaceType = genericInterfaces[i]; 
                
                // there is no guarantee that *all* the interfaces are generic
                if (interfaceType instanceof ParameterizedType) {
                    ParameterizedType genericInterface = (ParameterizedType) interfaceType;
                    
                    if (genericInterface.getRawType().equals(supertype)) {
                        return genericInterface;
                    }
                    
                }
                
            }
            
        }
        
        throw new AssertionError("Unable to find generic superclass information for class '"
                                 + clazz + "' and superclass/-interface '" + supertype + "'");
    }

    private static List<Class<?>> getActualAssignments(
            TypeVariable<?>[] typedClassTypeParams,
            Map<TypeVariable<?>, Class<?>> typeAssignments) {
        int numTypedClassTypeParams = typedClassTypeParams.length;
        List<Class<?>> actualAssignments = 
            new ArrayList<Class<?>>(numTypedClassTypeParams);
        
        // for entries that could not be resolved, null should be returned
        for (int i = 0; i < numTypedClassTypeParams; i++) {
            actualAssignments.add(typeAssignments.get(typedClassTypeParams[i]));
        }
        
        return actualAssignments;
    }    
    
}