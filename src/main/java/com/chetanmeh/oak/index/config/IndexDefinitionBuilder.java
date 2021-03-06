package com.chetanmeh.oak.index.config;

import java.util.Arrays;
import java.util.Map;

import javax.jcr.PropertyType;

import com.google.appengine.labs.repackaged.com.google.common.collect.Maps;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.index.PathFilter;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;
import static org.apache.jackrabbit.oak.plugins.memory.EmptyNodeState.EMPTY_NODE;
import static org.apache.jackrabbit.oak.plugins.memory.PropertyStates.createProperty;

public class IndexDefinitionBuilder {
    private final NodeBuilder builder = EMPTY_NODE.builder();
    private final Map<String, IndexRule> rules = Maps.newHashMap();
    private final Map<String, AggregateRule> aggRules = Maps.newHashMap();
    private final NodeBuilder indexRule;
    private NodeBuilder aggregateBuilder;

    public IndexDefinitionBuilder(){
        builder.setProperty(LuceneIndexConstants.COMPAT_MODE, 2);
        builder.setProperty("async", "async");
        builder.setProperty("type", "lucene");
        builder.setProperty(JCR_PRIMARYTYPE, "oak:QueryIndexDefinition", Type.NAME);
        indexRule = createChild(builder, LuceneIndexConstants.INDEX_RULES);
    }

    public IndexDefinitionBuilder evaluatePathRestrictions(){
        builder.setProperty(LuceneIndexConstants.EVALUATE_PATH_RESTRICTION, true);
        return this;
    }

    public IndexDefinitionBuilder includedPaths(String ... paths){
        builder.setProperty(createProperty(PathFilter.PROP_INCLUDED_PATHS, Arrays.asList(paths), Type.STRINGS));
        return this;
    }

    public IndexDefinitionBuilder excludedPaths(String ... paths){
        builder.setProperty(createProperty(PathFilter.PROP_EXCLUDED_PATHS, Arrays.asList(paths), Type.STRINGS));
        return this;
    }

    public NodeState build(){
        return builder.getNodeState();
    }

    //~--------------------------------------< IndexRule >

    public IndexRule indexRule(String type){
        IndexRule rule = rules.get(type);
        if (rule == null){
            rule = new IndexRule(createChild(indexRule, type), type);
            rules.put(type, rule);
        }
        return rule;
    }

    public static class IndexRule {
        private final NodeBuilder builder;
        private final NodeBuilder propertiesBuilder;
        private final String ruleName;
        private final Map<String, PropertyRule> props = Maps.newHashMap();

        private IndexRule(NodeBuilder builder, String type) {
            this.builder = builder;
            this.propertiesBuilder = createChild(builder, LuceneIndexConstants.PROP_NODE);
            this.ruleName = type;
        }

        public IndexRule indexNodeName(){
            builder.setProperty(LuceneIndexConstants.INDEX_NODE_NAME, true);
            return this;
        }

        public PropertyRule property(String name){
            PropertyRule propRule = props.get(name);
            if (propRule == null){
                propRule = new PropertyRule(this, createChild(propertiesBuilder, "prop" + props.size()), name);
                props.put(name, propRule);
            }
            return propRule;
        }

        public String getRuleName() {
            return ruleName;
        }
    }

    public static class PropertyRule {
        private final IndexRule indexRule;
        private final NodeBuilder builder;

        private PropertyRule(IndexRule indexRule, NodeBuilder builder, String name) {
            this.indexRule = indexRule;
            this.builder = builder;
            builder.setProperty(LuceneIndexConstants.PROP_NAME, name);
        }

        public PropertyRule useInExcerpt(){
            builder.setProperty(LuceneIndexConstants.PROP_USE_IN_EXCERPT, true);
            return this;
        }

        public PropertyRule analyzed(){
            builder.setProperty(LuceneIndexConstants.PROP_ANALYZED, true);
            return this;
        }

        public PropertyRule nodeScopeIndex(){
            builder.setProperty(LuceneIndexConstants.PROP_NODE_SCOPE_INDEX, true);
            return this;
        }

        public PropertyRule ordered(){
            builder.setProperty(LuceneIndexConstants.PROP_ORDERED, true);
            return this;
        }

        public PropertyRule ordered(String type){
            //This would throw an IAE if type is invalid
            int typeValue = PropertyType.valueFromName(type);
            builder.setProperty(LuceneIndexConstants.PROP_ORDERED, true);
            builder.setProperty(LuceneIndexConstants.PROP_TYPE, type);
            return this;
        }

        public PropertyRule propertyIndex(){
            builder.setProperty(LuceneIndexConstants.PROP_PROPERTY_INDEX, true);
            return this;
        }

        public PropertyRule nullCheckEnabled(){
            builder.setProperty(LuceneIndexConstants.PROP_NULL_CHECK_ENABLED, true);
            return this;
        }

        public PropertyRule notNullCheckEnabled(){
            builder.setProperty(LuceneIndexConstants.PROP_NOT_NULL_CHECK_ENABLED, true);
            return this;
        }

        public IndexRule enclosingRule(){
            return indexRule;
        }
    }

    //~--------------------------------------< Aggregates >

    public AggregateRule aggregateRule(String type){
        if (aggregateBuilder == null){
            aggregateBuilder = createChild(builder, LuceneIndexConstants.AGGREGATES);
        }
        AggregateRule rule = aggRules.get(type);
        if (rule == null){
            rule = new AggregateRule(createChild(aggregateBuilder, type));
            aggRules.put(type, rule);
        }
        return rule;
    }

    public AggregateRule aggregateRule(String primaryType, String ... includes){
        AggregateRule rule = aggregateRule(primaryType);
        for (String include : includes){
            rule.include(include);
        }
        return rule;
    }

    public static class AggregateRule {
        private final NodeBuilder builder;
        private final Map<String, Include> includes = Maps.newHashMap();

        private AggregateRule(NodeBuilder builder) {
            this.builder = builder;
        }

        public Include include(String includePath) {
            Include include = includes.get(includePath);
            if (include == null){
                include = new Include(createChild(builder, "include" + includes.size()));
                includes.put(includePath, include);
            }
            include.path(includePath);
            return include;
        }

        public static class Include {
            private final NodeBuilder builder;

            private Include(NodeBuilder builder) {
                this.builder = builder;
            }

            public Include path(String includePath) {
                builder.setProperty(LuceneIndexConstants.AGG_PATH, includePath);
                return this;
            }

            public Include relativeNode(){
                builder.setProperty(LuceneIndexConstants.AGG_RELATIVE_NODE, true);
                return this;
            }
        }
    }

    private static NodeBuilder createChild(NodeBuilder builder, String name){
        NodeBuilder result = builder.child(name);
        result.setProperty(JCR_PRIMARYTYPE, NT_UNSTRUCTURED, Type.NAME);
        return result;
    }
}
