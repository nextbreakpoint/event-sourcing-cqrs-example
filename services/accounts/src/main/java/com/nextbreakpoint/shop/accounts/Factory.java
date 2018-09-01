package com.nextbreakpoint.shop.accounts;

import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountController;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.delete.DeleteAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountController;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.insert.InsertAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsController;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.list.ListAccountsResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountController;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadAccountResponseMapper;
import com.nextbreakpoint.shop.accounts.controllers.load.LoadSelfAccountRequestMapper;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountRequest;
import com.nextbreakpoint.shop.accounts.model.DeleteAccountResponse;
import com.nextbreakpoint.shop.accounts.model.InsertAccountRequest;
import com.nextbreakpoint.shop.accounts.model.InsertAccountResponse;
import com.nextbreakpoint.shop.accounts.model.ListAccountsRequest;
import com.nextbreakpoint.shop.accounts.model.ListAccountsResponse;
import com.nextbreakpoint.shop.accounts.model.LoadAccountRequest;
import com.nextbreakpoint.shop.accounts.model.LoadAccountResponse;
import com.nextbreakpoint.shop.common.vertx.TemplateHandler;
import com.nextbreakpoint.shop.common.vertx.consumers.FailedRequestConsumer;
import com.nextbreakpoint.shop.common.vertx.consumers.OptionalConsumer;
import com.nextbreakpoint.shop.common.vertx.consumers.SimpleJsonConsumer;
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
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createInsertAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, InsertAccountRequest, InsertAccountResponse, String>builder()
                .withInputMapper(new InsertAccountRequestMapper())
                .withOutputMapper(new InsertAccountResponseMapper())
                .withController(new InsertAccountController(store))
                .onSuccess(new SimpleJsonConsumer(201))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createListAccountsHandler(Store store) {
        return TemplateHandler.<RoutingContext, ListAccountsRequest, ListAccountsResponse, String>builder()
                .withInputMapper(new ListAccountsRequestMapper())
                .withOutputMapper(new ListAccountsResponseMapper())
                .withController(new ListAccountsController(store))
                .onSuccess(new SimpleJsonConsumer(200))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse, Optional<String>>builder()
                .withInputMapper(new LoadAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new OptionalConsumer<>(new SimpleJsonConsumer(200)))
                .onFailure(new FailedRequestConsumer())
                .build();
    }

    public static Handler<RoutingContext> createLoadSelfAccountHandler(Store store) {
        return TemplateHandler.<RoutingContext, LoadAccountRequest, LoadAccountResponse,  Optional<String>>builder()
                .withInputMapper(new LoadSelfAccountRequestMapper())
                .withOutputMapper(new LoadAccountResponseMapper())
                .withController(new LoadAccountController(store))
                .onSuccess(new OptionalConsumer<>(new SimpleJsonConsumer(200)))
                .onFailure(new FailedRequestConsumer())
                .build();
    }
}
