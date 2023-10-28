package com.nextbreakpoint.blueprint.accounts;

import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountController;
import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountRequestMapper;
import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountResponse;
import com.nextbreakpoint.blueprint.accounts.operations.delete.DeleteAccountResponseMapper;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountController;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountRequestMapper;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountResponse;
import com.nextbreakpoint.blueprint.accounts.operations.insert.InsertAccountResponseMapper;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsController;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsRequest;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsRequestMapper;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsResponse;
import com.nextbreakpoint.blueprint.accounts.operations.list.ListAccountsResponseMapper;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountController;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountRequest;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountRequestMapper;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountResponse;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadAccountResponseMapper;
import com.nextbreakpoint.blueprint.accounts.operations.load.LoadSelfAccountRequestMapper;
import com.nextbreakpoint.blueprint.common.vertx.ErrorConsumer;
import com.nextbreakpoint.blueprint.common.vertx.JsonConsumer;
import com.nextbreakpoint.blueprint.common.vertx.NotFoundConsumer;
import com.nextbreakpoint.blueprint.common.vertx.TemplateHandler;
import io.vertx.core.Handler;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Optional;

public class Factory {
    private Factory() {}

    public static Handler<RoutingContext> createDeleteAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, DeleteAccountRequest, DeleteAccountResponse, String>builder()
                .withInputMapper(new DeleteAccountRequestMapper())
                .withController(new DeleteAccountController(store))
                .withOutputMapper(new DeleteAccountResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createInsertAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, InsertAccountRequest, InsertAccountResponse, String>builder()
                .withInputMapper(new InsertAccountRequestMapper())
                .withController(new InsertAccountController(store))
                .withOutputMapper(new InsertAccountResponseMapper())
                .onSuccess(new JsonConsumer(201))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createListAccountsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListAccountsRequest, ListAccountsResponse, String>builder()
                .withInputMapper(new ListAccountsRequestMapper())
                .withController(new ListAccountsController(store))
                .withOutputMapper(new ListAccountsResponseMapper())
                .onSuccess(new JsonConsumer(200))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse, Optional<String>>builder()
                .withInputMapper(new LoadAccountRequestMapper())
                .withController(new LoadAccountController(store))
                .withOutputMapper(new LoadAccountResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadSelfAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse,  Optional<String>>builder()
                .withInputMapper(new LoadSelfAccountRequestMapper())
                .withController(new LoadAccountController(store))
                .withOutputMapper(new LoadAccountResponseMapper())
                .onSuccess(new NotFoundConsumer<>(new JsonConsumer(200)))
                .onFailure(new ErrorConsumer())
                .build();
    }
}
