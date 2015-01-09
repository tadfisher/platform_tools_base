    /**
     * A placeholder fragment containing a simple view. This fragment
     * would include your content.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.${fragmentLayoutName}, container, false);
            return rootView;
        }
    }

    /**
     * A ad fragment containing a simple view.
     */
    public static class AdFragment extends Fragment {
        private <#if adNetwork == "admob">AdView<#elseif adNetwork == "dfp">PublisherAdView</#if> mAdView;

        public AdFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_ad, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);
            mAdView = (<#if adNetwork == "admob">AdView<#elseif adNetwork == "dfp">PublisherAdView</#if>) getView().findViewById(R.id.adView);
            <#if adNetwork == "admob">AdRequest<#elseif adNetwork == "dfp">PublisherAdRequest</#if> adRequest =
                    new <#if adNetwork == "admob">AdRequest<#elseif adNetwork == "dfp">PublisherAdRequest</#if>.Builder().build();
            mAdView.loadAd(adRequest);
        }

        /** Called when leaving the activity */
        @Override
        public void onPause() {
            if (mAdView != null) {
                mAdView.pause();
            }
            super.onPause();
        }

        /** Called when returning to the activity */
        @Override
        public void onResume() {
            super.onResume();
            if (mAdView != null) {
                mAdView.resume();
            }
        }

        /** Called before the activity is destroyed */
        @Override
        public void onDestroy() {
            if (mAdView != null) {
                mAdView.destroy();
            }
            super.onDestroy();
        }
    }
