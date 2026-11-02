package io.hotdealwave.extractor.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON contract. Evidence always uses paths relative to the analyzed repository. */
public final class Analysis {
    public static final String CONFIRMED = "확인됨";
    public static final String UNKNOWN = "확인 안 됨";
    public static final String ABSENT = "저장소에 근거 없음";

    public int schemaVersion = 1;
    public List<Service> services = new ArrayList<>();
    public List<Endpoint> endpoints = new ArrayList<>();
    public List<FeignClient> feignClients = new ArrayList<>();
    public List<FeignCall> feignCalls = new ArrayList<>();
    public List<Route> gatewayRoutes = new ArrayList<>();
    public List<AllowedPath> allowedPaths = new ArrayList<>();
    public List<Declaration> declarations = new ArrayList<>();
    public Map<String, Map<String, Integer>> checks = new LinkedHashMap<>();
    public List<Warning> warnings = new ArrayList<>();

    public record Fact<T>(T value, String status, List<String> evidence, String reason) {
        public static <T> Fact<T> known(T value, List<String> evidence) {
            return new Fact<>(value, CONFIRMED, evidence, null);
        }
        public static <T> Fact<T> unknown(List<String> evidence, String reason) {
            return new Fact<>(null, UNKNOWN, evidence, reason);
        }
        public boolean known() { return CONFIRMED.equals(status); }
    }
    public record Service(String directory, Fact<String> name, String status, List<String> evidence) {}
    public record Declaration(String kind, String status, List<String> evidence) {}
    public record Warning(String check, String status, List<String> evidence, String reason) {}
    public record AnnotationInfo(String name, Map<String, Fact<String>> arguments,
                                 String status, List<String> evidence) {}
    public record Field(String name, TypeView schema, List<AnnotationInfo> annotations,
                        Fact<String> jsonName, Fact<Boolean> jsonIgnored, String status, List<String> evidence) {}
    public static final class TypeView {
        public String type;
        public String qualifiedName;
        public String kind;
        public String status;
        public String reason;
        public List<String> evidence = new ArrayList<>();
        public boolean recursive;
        public List<TypeView> arguments = new ArrayList<>();
        public List<Field> fields = new ArrayList<>();
        public List<String> enumValues = new ArrayList<>();
    }
    public record Parameter(String name, String kind, String binding, TypeView schema,
                            List<AnnotationInfo> annotations, String status, List<String> evidence) {}
    public static final class Endpoint {
        public String id;
        public String service;
        public String controller;
        public String handler;
        public Fact<String> httpMethod;
        public Fact<String> path;
        public String status;
        public List<String> evidence;
        public List<Parameter> parameters = new ArrayList<>();
        public TypeView response;
        public List<ExternalPath> externalPaths = new ArrayList<>();
        public Fact<Boolean> tokenRequired;
    }
    public record ExternalPath(String routeId, String path, Fact<Boolean> tokenRequired,
                               String status, List<String> evidence) {}
    public record FeignClient(String id, String service, String type, Fact<String> name,
                              Fact<String> url, Fact<String> path, String status, List<String> evidence) {}
    public static final class FeignCall {
        public String id;
        public String clientId;
        public String service;
        public String handler;
        public Fact<String> httpMethod;
        public Fact<String> path;
        public boolean external;
        public Fact<String> target;
        public String status;
        public List<String> evidence;
    }
    public record Route(String id, Fact<String> uri, Fact<String> targetService,
                        List<Fact<String>> paths, Fact<String> rewritePath, String status, List<String> evidence) {}
    public record AllowedPath(String name, Fact<String> path, Fact<String> httpMethod,
                              String status, List<String> evidence) {}

    @SafeVarargs public static List<String> evidence(List<String>... lists) {
        var result = new java.util.LinkedHashSet<String>();
        for (var list : lists) result.addAll(list);
        return new ArrayList<>(result);
    }
}
