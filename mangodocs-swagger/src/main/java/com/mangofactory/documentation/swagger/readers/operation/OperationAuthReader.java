package com.mangofactory.documentation.swagger.readers.operation;

import com.mangofactory.documentation.spi.DocumentationType;
import com.mangofactory.documentation.service.model.builder.AuthorizationBuilder;
import com.mangofactory.documentation.service.model.builder.AuthorizationScopeBuilder;
import com.mangofactory.documentation.spi.service.OperationBuilderPlugin;
import com.mangofactory.documentation.spi.service.contexts.OperationContext;
import com.mangofactory.documentation.spi.service.contexts.AuthorizationContext;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.Authorization;
import com.wordnik.swagger.annotations.AuthorizationScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static com.google.common.collect.Lists.*;

@Component
public class OperationAuthReader implements OperationBuilderPlugin {

  private static final Logger LOG = LoggerFactory.getLogger(OperationAuthReader.class);
  @Override
  public void apply(OperationContext context) {

    AuthorizationContext authorizationContext = context.authorizationContext();

    HandlerMethod handlerMethod = context.getHandlerMethod();
    String requestMappingPattern = context.requestMappingPattern();
    List<com.mangofactory.documentation.service.model.Authorization> authorizations = newArrayList();

    if (null != authorizationContext) {
      authorizations = authorizationContext.getAuthorizationsForPath(requestMappingPattern);
    }

    ApiOperation apiOperationAnnotation = handlerMethod.getMethodAnnotation(ApiOperation.class);

    if (null != apiOperationAnnotation && null != apiOperationAnnotation.authorizations()) {
      Authorization[] authorizationAnnotations = apiOperationAnnotation.authorizations();
      if (authorizationAnnotations != null
              && authorizationAnnotations.length > 0
              && StringUtils.hasText(authorizationAnnotations[0].value())) {

        authorizations = newArrayList();
        for (Authorization authorization : authorizationAnnotations) {
          String value = authorization.value();
          AuthorizationScope[] scopes = authorization.scopes();
          List<com.mangofactory.documentation.service.model.AuthorizationScope> authorizationScopeList = newArrayList();
          for (AuthorizationScope authorizationScope : scopes) {
            String description = authorizationScope.description();
            String scope = authorizationScope.scope();
            authorizationScopeList.add(new AuthorizationScopeBuilder().scope(scope).description(description)
                    .build());
          }
          com.mangofactory.documentation.service.model.AuthorizationScope[] authorizationScopes = authorizationScopeList
                  .toArray(new com.mangofactory.documentation.service.model.AuthorizationScope[authorizationScopeList.size()]);
          com.mangofactory.documentation.service.model.Authorization authorizationModel =
                  new AuthorizationBuilder()
                          .type(value)
                          .scopes(authorizationScopes)
                          .build();
          authorizations.add(authorizationModel);
        }
      }
    }
    if (authorizations != null) {
      LOG.debug("Authorization count {} for method {}", authorizations.size(), handlerMethod.getMethod().getName());
      context.operationBuilder().authorizations(authorizations);
    }
  }

  @Override
  public boolean supports(DocumentationType delimiter) {
    return true;
  }
}