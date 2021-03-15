package com.nextbreakpoint.blueprint.accounts;

import com.nextbreakpoint.blueprint.accounts.controllers.delete.DeleteAccountController;
import com.nextbreakpoint.blueprint.accounts.controllers.delete.DeleteAccountRequestMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.delete.DeleteAccountResponseMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.insert.InsertAccountController;
import com.nextbreakpoint.blueprint.accounts.controllers.insert.InsertAccountRequestMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.insert.InsertAccountResponseMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.list.ListAccountsController;
import com.nextbreakpoint.blueprint.accounts.controllers.list.ListAccountsRequestMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.list.ListAccountsResponseMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.load.LoadAccountController;
import com.nextbreakpoint.blueprint.accounts.controllers.load.LoadAccountRequestMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.load.LoadAccountResponseMapper;
import com.nextbreakpoint.blueprint.accounts.controllers.load.LoadSelfAccountRequestMapper;
import com.nextbreakpoint.blueprint.accounts.model.*;
import com.nextbreakpoint.blueprint.common.vertx.DelegateConsumer;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createDeleteAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, DeleteAccountRequest, DeleteAccountResponse, String>builder()
                .withInputMapper(new DeleteAccountRequestMapper())
                .withOutputMapper(new DeleteAccountResponseMapper())
                .withController(new DeleteAccountController(store))
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createInsertAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, InsertAccountRequest, InsertAccountResponse, String>builder()
                .withInputMapper(new InsertAccountRequestMapper())
                .withOutputMapper(new InsertAccountResponseMapper())
                .withController(new InsertAccountController(store))
                .onSuccess(new JsonConsumer(201))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createListAccountsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListAccountsRequest, ListAccountsResponse, String>builder()
                .withInputMapper(new ListAccountsRequestMapper())
                .withOutputMapper(new ListAccountsResponseMapper())
                .withController(new ListAccountsController(store))
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse, Optional<String>>builder()
                .withInputMapper(new LoadAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new DelegateConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadSelfAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse,  Optional<String>>builder()
                .withInputMapper(new LoadSelfAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new DelegateConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
