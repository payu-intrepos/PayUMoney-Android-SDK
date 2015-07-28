package com.payUMoney.sdk.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.payUMoney.sdk.CobbocEvent;
import com.payUMoney.sdk.database.Cards;
import com.payUMoney.sdk.entity.Card;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link Manager} for {@link Card}s
 */
public class CardManager extends Manager<Card> {
    private static CardManager INSTANCE;
    private Cards db;
    int[] arr = new int[20];

    private CardManager(Context context) {
        super(context);
        db = Cards.getInstance(context);
        eventBus.register(this);
    }

    public static synchronized CardManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CardManager(context);
        }
        return INSTANCE;
    }

    /**
     * Get a {@link Card} from a {@link org.json.JSONObject}
     */
    public static Card getCard(JSONObject jsonCardObject) throws JSONException {
        Card card = new Card();
        card.setId(jsonCardObject.getLong("storeCardInfoId"));
        card.setName(jsonCardObject.getString("nameOnCard"));
        card.setNumber(jsonCardObject.getString("cardNumber"));
        card.setLabel(jsonCardObject.getString("cardName"));
        card.setToken(jsonCardObject.getString("cardToken"));
        card.setMode(jsonCardObject.getString("cardMode"));
        return card;
    }

    /**
     * Get a {@link Card} from a database {@link android.database.Cursor}
     */
    public static Card getCard(Cursor values) {
        Card card = new Card();

        card.setId(values.getLong(values.getColumnIndex(Cards.COL_ID)));
        card.setName(values.getString(values.getColumnIndex(Cards.COL_NAME)));
        card.setNumber(values.getString(values.getColumnIndex(Cards.COL_NUMBER)));
        card.setLabel(values.getString(values.getColumnIndex(Cards.COL_LABEL)));
        card.setToken(values.getString(values.getColumnIndex(Cards.COL_TOKEN)));
        card.setMode(values.getString(values.getColumnIndex(Cards.COL_MODE)));
        return card;
    }

    /**
     * Get {@link android.content.ContentValues} for database insertion from a {@link Card}
     */
    public static ContentValues getContentValues(Card card) {
        ContentValues values = new ContentValues();
        values.put(Cards.COL_ID, card.getId());
        values.put(Cards.COL_NAME, card.getName());
        values.put(Cards.COL_NUMBER, card.getNumber());
        values.put(Cards.COL_LABEL, card.getLabel());
        values.put(Cards.COL_TOKEN, card.getToken());
        values.put(Cards.COL_MODE, card.getMode());
        return values;
    }

    /**
     * {@link de.greenrobot.event.EventBus} listener for {@link com.payu.payumoney.CobbocEvent}
     */
//    public void onEventBackgroundThread(CobbocEvent event) {
//        if (event.getType() == CobbocEvent.CARD_DELETED) {
//            if (event.getStatus()) {
//                deleteSucceeded((Integer) event.getValue());
//            } else {
//                deleteFailed((Integer) event.getValue());
//                eventBus.post(new CobbocEvent(CobbocEvent.DATABASE_UPDATED, true));
//            }
//        }
//    }
    public List<Card> getCards() {
        Cursor result = findAll();
        List<Card> cards = new ArrayList<Card>();
        while (result.getCount() > 0 && !result.isLast()) {
            result.moveToNext();
            cards.add(getCard(result));
        }
        result.close();
        return cards;
    }

    @Override
    protected Cards getDb() {
        db = Cards.getInstance(mContext);
        return db;
    }

    @Override
    public void deleteOnServer(int id) {
        session.deleteCard(id);
        getDb().markDeleted(id);
        eventBus.post(new CobbocEvent(CobbocEvent.DATABASE_UPDATED, true));
    }
}
