package com.tbg.bitpaypos.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import io.triangle.reader.PaymentCard;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * A compound UI component which presents information stored in a {@link PaymentCard} object. {@link PaymentCard} is
 * obtained via the Triangle API and stores information of a payment card. This class simply presents the information
 * to the user in a user interface.
 */
public class CardView extends LinearLayout
{
    private TextView cardholder;
    private TextView accountNumber;
    private TextView expiryFrom;
    private TextView expiryTo;
    private TextView brandName;
    private TextView preferredName;
    private RelativeLayout root;
    private ImageView closeButton;
    private ImageView cardLogo;
    private Random random;

    private LinearLayout parent;

    public CardView(final LinearLayout parent, PaymentCard scannedCard, Context context)
    {
        super(context);

        this.parent = parent;
        this.random = new Random();

        // Inflate the layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.cardview, this, true);

        // Center this element in its parent
        this.setGravity(Gravity.CENTER);

        // Set the background of the view based on the index of the card. We have 3 colors and
        // will choose the color to use based on % 3
        this.root = (RelativeLayout)view.findViewById(R.id.cardview_relativeLayout_root);

        this.accountNumber = (TextView)view.findViewById(R.id.cardview_textView_account_number);
        this.cardholder = (TextView)view.findViewById(R.id.cardview_textView_cardholder);
        this.expiryFrom = (TextView)view.findViewById(R.id.cardview_textView_expiry_from);
        this.expiryTo = (TextView)view.findViewById(R.id.cardview_textView_expiry_to);
        this.brandName = (TextView)view.findViewById(R.id.cardview_textView_brand);
        this.preferredName = (TextView)view.findViewById(R.id.cardview_textView_preferred_name);
        this.closeButton = (ImageView)view.findViewById(R.id.cardview_imageView_close);
        this.cardLogo = (ImageView)view.findViewById(R.id.cardview_imageView_logo);

        // Load values from the card
        this.setValuesFromCard(scannedCard);

        // Choose background color and card image based on brand
        this.setBackgroundColorAndCardLogo(scannedCard);

        // Remove the element from the parent on tap
        this.closeButton.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN)
                {
                    root.animate()
                            .translationX(root.getWidth())
                            .alpha(0)
                            .setDuration(view.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime))
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation)
                                {
                                    // Remove the element once the animation is over
                                    parent.removeView(CardView.this);
                                }
                            });

                    // Mark the event as handled
                    return true;
                }

                return false;
            }
        });
    }

    private void setBackgroundColorAndCardLogo(PaymentCard scannedCard)
    {
        // Try to determine the card's brand
        CardBrand brand = CardBrand.Other;
        String scannedBrand = scannedCard.getCardBrand();

        if (scannedBrand != null)
        {
            scannedBrand = scannedBrand.toLowerCase();

            if (scannedBrand.contains("visa"))
            {
                brand = CardBrand.Visa;
            }
            else if (scannedBrand.contains("interac"))
            {
                brand = CardBrand.Interac;
            }
            else if (scannedBrand.contains("master"))
            {
                brand = CardBrand.MasterCard;
            }
        }

        // Choose background color based on brand
        int backgroundColor;
        switch (brand)
        {
            case Visa:
                backgroundColor = this.chooseRandomColor(new String[] { "#5CA4EE", "#65C284", "#E387B5", "#FF9000" });
                break;
            case Interac:
                backgroundColor = this.chooseRandomColor(new String[] { "#C43E3A", "#9783A7", "#EC8122" });
                break;
            case MasterCard:
                // Purposeful fall through case. We choose the same colors for MasterCard and unknown brands
            default:
                backgroundColor = this.chooseRandomColor(new String[] { "#676767", "#893333", "#335389", "#0DA060" });
                break;
        }

        this.root.setBackgroundColor(backgroundColor);

        // Now choose logo
        double logoPaddingRightDp = 12.1;
        double logoPaddingBottomDp = 0;
        switch (brand)
        {
            case Visa:
                this.cardLogo.setImageResource(R.drawable.ic_visa);
                logoPaddingBottomDp = 12.1;
                break;
            case Interac:
                this.cardLogo.setImageResource(R.drawable.ic_interac);
                logoPaddingBottomDp = 9.25;
                break;
            case MasterCard:
                this.cardLogo.setImageResource(R.drawable.ic_mastercard);
                logoPaddingBottomDp = 8.25;
                break;
            default:
                // Hide the logo if we don't recognise the brand
                this.cardLogo.setVisibility(GONE);
                break;
        }

        // Set padding in absolute pixels if the card brand logo is visible
        if (this.cardLogo.getVisibility() == VISIBLE)
        {
            float density = this.getContext().getResources().getDisplayMetrics().density;

            this.cardLogo.setPadding(
                    0, // left
                    0, // top
                    (int)(logoPaddingRightDp * density), // right
                    (int)(logoPaddingBottomDp * density) // bottom
            );
        }
    }

    private int chooseRandomColor(String[] items)
    {
        return Color.parseColor(items[this.random.nextInt(items.length)]);
    }

    /**
     * Reads the information from the payment card and sets various UI components so that the end user can see
     * the values.
     * @param scannedCard object representing the data scanned from a credit card via NFC.
     */
    private void setValuesFromCard(PaymentCard scannedCard)
    {
        SimpleDateFormat shortFormat = new SimpleDateFormat("dd/MM/yy");
        Date activationDate = scannedCard.getActivationDate();
        Date expiryDate = scannedCard.getExpiryDate();
        String accountNumber = scannedCard.getLastFourDigits();
        String brandName = scannedCard.getCardBrand();
        String preferredName = scannedCard.getCardPreferredName();
        String cardholderName = scannedCard.getCardholderName();

        this.accountNumber.setText(accountNumber);

        // Since various card manufacturers may choose not to include some information on the payment card, we check
        // the obtained values from the card against null.
        this.expiryTo.setText(expiryDate == null ? "" : shortFormat.format(expiryDate));
        this.expiryFrom.setText(activationDate == null ? "" : shortFormat.format(activationDate));
        this.brandName.setText(brandName == null ? "" : brandName.toUpperCase());
        this.preferredName.setText(preferredName == null ? "" : preferredName);
        this.cardholder.setText(cardholderName == null ? "" : cardholderName);
    }

    private enum CardBrand
    {
        Visa,
        MasterCard,
        Interac,
        Other
    }
}
