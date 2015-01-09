    /**
     * A fragment containing the game logic.
     * Loads an interstitial at the beginning of the game, and shows between retries.
     */
    public static class GameFragment extends Fragment {
        private <#if adNetwork == "admob">InterstitialAd<#elseif adNetwork == "dfp">PublisherInterstitialAd</#if> mInterstitialAd;
        private CountDownTimer mCountDownTimer;
        private Button mRetryButton;

        public GameFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.${fragmentLayoutName}, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            // Initialize the game and the ad.
            super.onActivityCreated(savedInstanceState);
            initButton();
            initTimer();
            initAd();
        }

       @Override
        public void onResume() {
            // Initialize the timer if it hasn't been initialized yet.
            // Start the game.
            super.onResume();
            if (mCountDownTimer == null) {
                initTimer();
            }
            startGame();
        }

        @Override
        public void onPause() {
            // Cancel the timer if the game is paused.
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
            super.onPause();
        }

        private void initAd() {
            // Create the InterstitialAd and set the adUnitId.
            mInterstitialAd = new <#if adNetwork == "admob">InterstitialAd<#elseif adNetwork == "dfp">PublisherInterstitialAd</#if>(getActivity());
            // Defined in values/strings.xml
            mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
        }

        private void displayAd() {
            // Show the ad if it's ready. Otherwise toast and restart the game.
            if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                Toast.makeText(getActivity(), "Ad did not load", Toast.LENGTH_SHORT).show();
                startGame();
            }
        }

        private void startGame() {
            // Hide the retry button, load the ad, and start the timer.
            mRetryButton.setVisibility(View.INVISIBLE);
            <#if adNetwork == "admob">AdRequest<#elseif adNetwork == "dfp">PublisherAdRequest</#if> adRequest =
                    new <#if adNetwork == "admob">AdRequest<#elseif adNetwork == "dfp">PublisherAdRequest</#if>.Builder().build();
            mInterstitialAd.loadAd(adRequest);
            mCountDownTimer.start();
        }

        private void initButton() {
            // Create the "retry" button, which tries to show an interstitial between game plays.
            mRetryButton = ((Button) getView().findViewById(R.id.retry_button));
            mRetryButton.setVisibility(View.INVISIBLE);
            mRetryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayAd();
                }
            });
        }

        private void initTimer() {
            // Create the game timer, which counts down to the end of the level
            // and shows the "retry" button.
            final TextView textView = ((TextView) getView().findViewById(R.id.timer));
            mCountDownTimer = new CountDownTimer(4000, 1000) {
                @Override
                public void onTick(long millisUnitFinished) {
                    textView.setText("seconds remaining: " + millisUnitFinished / 1000);
                }

                @Override
                public void onFinish() {
                    textView.setText("done!");
                    mRetryButton.setVisibility(View.VISIBLE);
                }
            };
        }

    }