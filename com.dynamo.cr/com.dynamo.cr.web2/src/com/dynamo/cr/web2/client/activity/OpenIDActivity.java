package com.dynamo.cr.web2.client.activity;

import com.dynamo.cr.web2.client.ClientFactory;
import com.dynamo.cr.web2.client.Defold;
import com.dynamo.cr.web2.client.LoginInfo;
import com.dynamo.cr.web2.client.TokenExchangeInfo;
import com.dynamo.cr.web2.client.place.DashboardPlace;
import com.dynamo.cr.web2.client.place.OpenIDPlace;
import com.dynamo.cr.web2.client.ui.OpenIDView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class OpenIDActivity extends AbstractActivity implements
        OpenIDView.Presenter {
    private String loginToken;
    private TokenExchangeInfo tokenExchangeInfo;
    private ClientFactory clientFactory;

    public OpenIDActivity(OpenIDPlace place, ClientFactory clientFactory) {
        loginToken = place.getLoginToken();
        this.clientFactory = clientFactory;
    }

    private Place getNewPlace() {
        final Defold defold = clientFactory.getDefold();
        String token = Defold.getCookie("afterLoginPlaceToken");
        Defold.removeCookie("afterLoginPlaceToken");

        Place newPlace = null;
        if (token != null) {
            newPlace = defold.getHistoryMapper().getPlace(token);
        }

        if (newPlace == null) {
            newPlace = new DashboardPlace();
        }
        return newPlace;
    }

    private void loadAsciiDoc(final OpenIDView view, String name) {
        view.setLoading(true);
        view.clear();

        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, "/site/" + name + ".html");
        try {
            builder.sendRequest("", new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    int status = response.getStatusCode();
                    if (status == 200) {
                        view.setLoading(false);
                        view.setText(response.getText());

                    } else {
                        view.setLoading(false);
                        clientFactory.getDefold().showErrorMessage("Unable to load document");
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    view.setLoading(false);
                    clientFactory.getDefold().showErrorMessage("Unable to load document");
                }
            });
        } catch (RequestException e) {
            view.setLoading(false);
            clientFactory.getDefold().showErrorMessage("Unable to load document");
        }
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        final Place newPlace = getNewPlace();

        final OpenIDView openIDView = clientFactory.getOpenIDView();
        containerWidget.setWidget(openIDView.asWidget());
        openIDView.setPresenter(this);

        final Defold defold = clientFactory.getDefold();

        loadAsciiDoc(openIDView, "eula");

        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
                defold.getUrl() + "/login/openid/exchange/" + loginToken);
        builder.setHeader("Accept", "application/json");

        try {
            builder.sendRequest("", new RequestCallback() {

                @Override
                public void onResponseReceived(Request request,
                        Response response) {
                    int status = response.getStatusCode();
                    if (status == 200) {
                        tokenExchangeInfo = TokenExchangeInfo.getResponse(response.getText());

                        if (tokenExchangeInfo.getType().equals("SIGNUP")) {
                            openIDView.showConfirmRegistrationPanel();
                            openIDView.setConfirmRegistrationData(tokenExchangeInfo.getFirstName(), tokenExchangeInfo.getLastName(), tokenExchangeInfo.getEmail());
                        } else if (tokenExchangeInfo.getType().equals("LOGIN")) {
                            openIDView.showLoginSuccessfulPanel();
                            String firstName = tokenExchangeInfo.getFirstName();
                            String lastName = tokenExchangeInfo.getLastName();
                            String email = tokenExchangeInfo.getEmail();
                            defold.loginOk(firstName, lastName, email, tokenExchangeInfo.getAuthCookie(), tokenExchangeInfo.getUserId());
                            clientFactory.getPlaceController().goTo(newPlace);
                        } else {
                            defold.showErrorMessage("Invalid server response");
                        }

                    } else {
                        defold.showErrorMessage("Login failed: " + response.getText());
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    defold.showErrorMessage("Network error");
                }
            });
        } catch (RequestException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register(String registrationKey) {
        final Defold defold = clientFactory.getDefold();

        if (registrationKey.isEmpty()) {
            defold.showErrorMessage("You need a registration key for alpha access.");
            return;
        }
        RequestBuilder builder = new RequestBuilder(RequestBuilder.PUT, defold.getUrl() + "/login/openid/register/" + loginToken + "?key=" + registrationKey);
        builder.setHeader("Accept", "application/json");

        try {
            builder.sendRequest("", new RequestCallback() {

                @Override
                public void onResponseReceived(Request request, Response response) {
                    int status = response.getStatusCode();
                    if (status == 200) {
                        LoginInfo loginInfo = LoginInfo.getResponse(response.getText());
                        defold.loginOk(loginInfo.getFirstName(), loginInfo.getLastName(), loginInfo.getEmail(), loginInfo.getAuth(), loginInfo.getUserId());
                        clientFactory.getPlaceController().goTo(new DashboardPlace());
                    } else {
                        defold.showErrorMessage("Registration failed: " + response.getText());
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    defold.showErrorMessage("Network error");
                }

            });
        } catch (RequestException e) {
            defold.showErrorMessage("Network error");
        }
    }
}
