package com.loopytime.im;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.loopytime.external.TouchImageView;
import com.loopytime.external.videotrimmer.utils.Screen;
import com.loopytime.external.videotrimmer.view.CircleViewAnim;
import com.loopytime.external.videotrimmer.view.DotsView;
import com.loopytime.helper.Utils;
import com.loopytime.im.status.FeedObject;
import com.loopytime.utils.Constants;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.loopytime.helper.Utils.feedMediaExist;
import static com.loopytime.helper.Utils.isInteger;
import static com.loopytime.im.WhatStatusFragmentFeedProfile.formatDate;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FeedDisplayFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FeedDisplayFragment#} factory method to
 * create an instance of this fragment.
 */
public class FeedDisplayFragment extends Fragment implements ExoPlayer.EventListener {
    CircleImageView avatarView;
    TextView likeButton;
    View likeButtonfrm;
    RecyclerView listLikes;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");


    @Override
    public void onResume() {
        super.onResume();
        if (feed == null) return;
        if (isvid && ( player == null)) {
            initializePlayer(feed.media);
        }
    }
public void playVid(){
    if (feed == null) return;
    if (isvid && ( player == null)) {
        initializePlayer(feed.media);
    }
}
    @Override
    public void onPause() {
        super.onPause();
        System.out.println("hjhjhjs pause");
       // if (Util.SDK_INT <= 23) {
            releasePlayer();
        //}

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    public void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
            trackSelector = null;
            //trackSelectionHelper = null;
            //eventLogger = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }



    void pauseMedia() {
        releasePlayer();
    }

    Calendar c = Calendar.getInstance();
    boolean isvid = false;

    void resetFeed() {
        feed = null;
    }

    boolean isLocal = false;

    public void changeMedia(FeedObject feed) {
        this.feed = feed;

//            UserVM user = users().get(Long.parseLong(feed.uid));
  //          avatarView.bind(user);
        Glide.with(this).applyDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.temp)).load(feed.upic).
                into(avatarView);
            uName.setText(feed.uname);

        isLocal = isInteger(feed.created_at);
        try {

                c.setTimeInMillis(format.parse(feed.created_at).getTime() + Calendar.getInstance().get(Calendar.ZONE_OFFSET));
                uDate.setText(formatDate(c, false));


        } catch (ParseException e) {
            uDate.setText(e.getMessage());
            e.printStackTrace();
        }
        ((TextView) res.findViewById(R.id.ttl_views)).setText(feed.comment_count);
        ((TextView) res.findViewById(R.id.likeButton)).setText(feed.reaction_count);
        //imageHandler.sendEmptyMessage(0);

        //imageHandler.removeCallbacks(imgRunnable);


        releasePlayer();
        //(res.findViewById(R.id.vis_lay)).setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(feed.text1) && !feed.text1.equalsIgnoreCase("null")) {
            isvid = false;
            (res.findViewById(R.id.nameEdit)).setVisibility(View.GONE);

            simpleExoPlayerView.setVisibility(View.GONE);
            transitionView.setVisibility(View.GONE);

            res.findViewById(R.id.text1).setVisibility(View.VISIBLE);
            nameEdit.setVisibility(View.GONE);
            showText(feed.text1);
            return;
        }
        res.findViewById(R.id.text1).setVisibility(View.GONE);
        if (feed.media_type.startsWith("vid_")) {
            isvid = true;


            initializePlayer(feed.media);
            simpleExoPlayerView.setVisibility(View.VISIBLE);
            transitionView.setVisibility(View.GONE);
        } else {

            isvid = false;
            if (feedMediaExist(feed.media)) {
                showImage(Utils.getFeedUrl(feed.media));
            } else
                showImage(ur + feed.media);
            simpleExoPlayerView.setVisibility(View.GONE);
            transitionView.setVisibility(View.VISIBLE);
        }


        nameEdit.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(feed.text2) && !TextUtils.isEmpty(feed.text2.replaceAll(" ", "").trim()))
            nameEdit.setText(feed.text2);
        else
            nameEdit.setVisibility(View.GONE);

    }

    public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
    public static final String DRM_LICENSE_URL = "drm_license_url";
    public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";
    public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

    public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
    public static final String EXTENSION_EXTRA = "extension";

    public static final String ACTION_VIEW_LIST =
            "com.google.android.exoplayer.demo.action.VIEW_LIST";
    public static final String URI_LIST_EXTRA = "uri_list";
    public static final String EXTENSION_LIST_EXTRA = "extension_list";

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final CookieManager DEFAULT_COOKIE_MANAGER;

    //private RtmpDataSource.RtmpDataSourceFactory rtmpDataSourceFactory;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    private Handler mainHandler;
    //  private EventLogger eventLogger;
    private SimpleExoPlayerView simpleExoPlayerView;
    // private LinearLayout debugRootView;
    //private TextView debugTextView;
    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private DefaultTrackSelector trackSelector;
    //  private TrackSelectionHelper trackSelectionHelper;
    // private DebugTextViewHelper debugViewHelper;
    private boolean needRetrySource;
    private TrackGroupArray lastSeenTrackGroupArray;

    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;
    // EmojiLikeTouchDetector emojiLikeTouchDetector;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "feed";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters

    FeedObject feed = null;

    private OnFragmentInteractionListener mListener;

    public FeedDisplayFragment() {
        // Required empty public constructor
    }

    private TouchImageView transitionView;
    String ur = Constants.NODE_URL+"pic_st/";

    int clr[];

    void showText(String txt) {


    }


    void showImage(String path) {

        if (getActivity() == null || isDetached()) return;
        Glide.with(this).
                load((path
                )).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                return false;
            }
        }).into(transitionView);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param
     * @param
     * @return A new instance of fragment DisplayFragment.
     */
    // TODO: Rename and change types and number of parameters
/*    public static FeedDisplayFragment newInstance(FeedObject param1, WhatStatusFragment.WhatStatusObj param2) {
        FeedDisplayFragment fragment = new FeedDisplayFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
     //   clr = getResources().getIntArray(R.array.clr);

    }

    View res;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return res = inflater.inflate(R.layout.fragment_display_feed, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    private int CLICK_ACTION_THRESHOLD = 200;
    private float startX;
    private float startY;

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return ApplicationClass.getInstance()
                .buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @return A new HttpDataSource factory.
     * @para2m useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     * DataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return ApplicationClass.getInstance()
                .buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    TextView nameEdit, uName, uDate;
    View rply_lay;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setLikesDatList();
        rply_lay = res.findViewById(R.id.rply_lay);
        vDotsView = (DotsView) res.findViewById(R.id.vDotsView);
        vCircle = (CircleViewAnim) res.findViewById(R.id.vCircle);
        ivStar = (ImageView) res.findViewById(R.id.ivStar);

        rply_lay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FeedDetailActivity) getActivity()).onHideFrag();
                return;
            }

        });
        res.findViewById(R.id.reUpload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                return;
            }

        });
        likeButton = res.findViewById(R.id.likeButton);
        likeButtonfrm = res.findViewById(R.id.likeButtonfrm);
        transitionView = (TouchImageView) res.findViewById(R.id.transition);
    /*    transitionView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });*/


        res.findViewById(R.id.ttl_views_frm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent1 = new Intent(getActivity(), FeedDetailActivity.class);
////                intent1.putExtra("feed", feed);
////                startActivityForResult(intent1, 101);
                ((FeedDetailActivity) getActivity()).onHideFrag();
            }
        });

        likeButtonfrm.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        likeButtonfrm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  WhatStatusFragment.loadData = false;
                Intent intent = new Intent(getActivity(), FeedLikes.class);
                intent.putExtra("post_id", feed._id);
                startActivity(intent);

            }
        });


        shouldAutoPlay = true;


        nameEdit = (TextView) res.findViewById(R.id.nameEdit);
        uName = (TextView) res.findViewById(R.id.uName);
        uDate = (TextView) res.findViewById(R.id.uDate);
        avatarView = (CircleImageView) res.findViewById(R.id.avatarView);
        clearResumePosition();
        mediaDataSourceFactory = buildDataSourceFactory(true);
        //rtmpDataSourceFactory = new RtmpDataSource.RtmpDataSourceFactory();
        mainHandler = new Handler();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }
        simpleExoPlayerView = (SimpleExoPlayerView) res.findViewById(R.id.player_view);
        //simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.setUseController(false);
        simpleExoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        simpleExoPlayerView.requestFocus();


    }


    int[] arrLikes = {R.drawable.ic_like, R.drawable.ic_haha, R.drawable.ic_love, R.drawable.ic_sad, R.drawable.ic_wow, R.drawable.ic_angry, R.drawable.ic_ya};

    void setLikesDatList() {


        listLikes = res.findViewById(R.id.listLikes);
        listLikes.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        listLikes.setAdapter(new RecyclerView.Adapter<ViewHolderLikes>() {
            @NonNull
            @Override
            public ViewHolderLikes onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

                return new ViewHolderLikes(LayoutInflater.from(getActivity()).inflate(R.layout.item_likesmedium, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolderLikes holder, int position) {
                holder.imv.setImageResource(arrLikes[position]);
            }

            @Override
            public int getItemCount() {
                return arrLikes.length;
            }
        });
    }

    class ViewHolderLikes extends RecyclerView.ViewHolder {
        ImageView imv;

        public ViewHolderLikes(View itemView) {
            super(itemView);
            imv = (ImageView) itemView.findViewById(R.id.ic_img);
            //  imv.getLayoutParams().height = Screen.dp(30);
            //imv.getLayoutParams().width = Screen.dp(30);
            imv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    likeAnimation(getAdapterPosition());

                }
            });
        }
    }

    private void initializePlayer(String path) {
        isLocal = isInteger(feed.created_at);
if (getActivity()==null)return;
        Intent intent = getActivity().getIntent();
        boolean needNewPlayer = player == null;
        if (needNewPlayer) {
            TrackSelection.Factory adaptiveTrackSelectionFactory =
                    new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
            trackSelector = new DefaultTrackSelector(adaptiveTrackSelectionFactory);
            // trackSelectionHelper = new TrackSelectionHelper(trackSelector, adaptiveTrackSelectionFactory);
            lastSeenTrackGroupArray = null;
            //   eventLogger = new EventLogger(trackSelector);

            DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;


            boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
            @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode =
                  ApplicationClass.getInstance().useExtensionRenderers()
                            ? (preferExtensionDecoders ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                            : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                            : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;

    /*  @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode =
              ((Controller) getApplication()).useExtensionRenderers()
                      ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
                      : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
    */
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(getActivity(),
                    null, extensionRendererMode);

            player = ExoPlayerFactory.newSimpleInstance(getActivity(), trackSelector);
            player.addListener(this);
            // player.addListener(eventLogger);
            //player.setAudioDebugListener(eventLogger);
            //player.setVideoDebugListener(eventLogger);
            //player.setMetadataOutput(eventLogger);

            simpleExoPlayerView.setPlayer(player);
            player.setPlayWhenReady(shouldAutoPlay);
          /*  debugViewHelper = new DebugTextViewHelper(player, debugTextView);
            debugViewHelper.start();*/
        }
        if (needNewPlayer || needRetrySource) {
            //   String action = intent.getAction();
//            Uri[] uris = {Uri.parse("http://97.74.4.59:85/" + getIntent().getStringExtra("file") + "/" + getIntent().getStringExtra("file") + ".m3u8")};
            //Uri.parse("http://97.74.4.59:85/" + getIntent().getStringExtra("file") + "/" + getIntent().getStringExtra("file") + ".m3u8")};
            String urlM;

            if (feedMediaExist(feed.media)) {

                urlM = Utils.getFeedUrl(feed.media);
            } else
                urlM = ur + path;//getSharedPreferences("wall", Context.MODE_PRIVATE).getString("explore","");
            Uri[] uris = {Uri.parse(urlM)};

            String[] extensions = {null};
            if (Util.maybeRequestReadExternalStoragePermission(getActivity(), uris)) {
                // The player will be reinitialized if the permission is granted.
                return;
            }
            MediaSource[] mediaSources = new MediaSource[uris.length];
            for (int i = 0; i < uris.length; i++) {
                mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
            }
            MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
                    : new ConcatenatingMediaSource(mediaSources);
            resumeWindow = player.getCurrentWindowIndex();
            resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
                    : C.TIME_UNSET;
            player.seekTo(resumeWindow, resumePosition);
            player.prepare(mediaSource, false, false);
            needRetrySource = false;
            updateButtonVisibilities();
        }
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                mainHandler, null);
        /// return new RtmpDataSource()HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null);

    }


    private void updateButtonVisibilities() {
        //debugRootView.removeAllViews();

        /*retryButton.setVisibility(needRetrySource ? View.VISIBLE : View.GONE);
         *///debugRootView.addView(retryButton);

        if (player == null) {
            return;
        }

        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return;
        }

   /*     for (int i = 0; i < mappedTrackInfo.length; i++) {
            TrackGroupArray trackGroups = mappedTrackInfo.getTrackGroups(i);
            if (trackGroups.length != 0) {
                Button button = new Button(getActivity());
                int label;
                switch (player.getRendererType(i)) {
                    case C.TRACK_TYPE_AUDIO:
                        label = R.string.audio;
                        break;
                    case C.TRACK_TYPE_VIDEO:
                        label = R.string.video;
                        break;
                    case C.TRACK_TYPE_TEXT:
                        label = R.string.text;
                        break;
                    default:
                        continue;
                }
                button.setText(label);
                button.setTag(i);
                //  button.setOnClickListener(this);
                //debugRootView.addView(button, debugRootView.getChildCount() - 1);
            }
        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        updateButtonVisibilities();
        if (trackGroups != lastSeenTrackGroupArray) {
            MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
            if (mappedTrackInfo != null) {
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_VIDEO)
                        == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    showToast(getString(R.string.error_unsupported_video));
                }
                if (mappedTrackInfo.getTrackTypeRendererSupport(C.TRACK_TYPE_AUDIO)
                        == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                    showToast(getString(R.string.error_unsupported_audio));
                }
            }
            lastSeenTrackGroupArray = trackGroups;
        }
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == ExoPlayer.STATE_ENDED) {
            //hasStarted = false;
            //showControls();
            //changeMedia();
        }
        if (playbackState == ExoPlayer.STATE_BUFFERING) {
         /*   if(countDownTimer!=null)
                countDownTimer.cancel();
        */
        }
        if (playbackState == ExoPlayer.STATE_BUFFERING) {
        /*    if(countDownTimer!=null)
                countDownTimer.cancel();
        */
            //  progressBar.setProgress((int) player.getCurrentPosition());
            // Toast.makeText(getActivity(), ""+player.getDuration(), Toast.LENGTH_SHORT).show();
        }
        if (playbackState == ExoPlayer.STATE_READY) {
            // Toast.makeText(getActivity(), "kk" + player.getDuration(), Toast.LENGTH_SHORT).show();
            videoTimer();

        }
        updateButtonVisibilities();
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    void videoTimer() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        //Toast.makeText(getActivity(), "errr"+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        String errorString = null;
        if (e.type == ExoPlaybackException.TYPE_RENDERER) {
            Exception cause = e.getRendererException();
            if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                // Special case for decoder initialization failures.
                MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                        (MediaCodecRenderer.DecoderInitializationException) cause;
                if (decoderInitializationException.decoderName == null) {
                    if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                        errorString = getString(R.string.error_querying_decoders);
                    } else if (decoderInitializationException.secureDecoderRequired) {
                        errorString = getString(R.string.error_no_secure_decoder,
                                decoderInitializationException.mimeType);
                    } else {
                        errorString = getString(R.string.error_no_decoder,
                                decoderInitializationException.mimeType);
                    }
                } else {
                    errorString = getString(R.string.error_instantiating_decoder,
                            decoderInitializationException.decoderName);
                }
            }
        }
        if (errorString != null) {
            showToast(errorString);
        }
        needRetrySource = true;
        if (isBehindLiveWindow(e)) {
            clearResumePosition();
            initializePlayer(feed.media);
        } else {
            updateResumePosition();
            updateButtonVisibilities();
            //  showControls();
        }
    }

    private void showToast(String message) {
        Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        if (needRetrySource) {
            // This will only occur if the user has performed a seek whilst in the error state. Update the
            // resume position so that if the user then retries, playback will resume from the position to
            // which they seeked.
            updateResumePosition();
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    DotsView vDotsView;
    CircleViewAnim vCircle;
    ImageView ivStar;

    void likeAnimation(int pos) {

        ((FeedDetailActivity) getActivity()).updateStatusReaction("" + pos, ((TextView) res.findViewById(R.id.likeButton)));
        ivStar.setImageResource(arrLikes[pos]);
        vCircle.setInnerCircleRadiusProgress(0);
        vCircle.setOuterCircleRadiusProgress(0);
        vDotsView.setCurrentProgress(0);

        AnimatorSet animatorSet = new AnimatorSet();
 /*       ObjectAnimator starScaleYAnimator = ObjectAnimator.ofFloat(ivStar, ImageView.SCALE_Y, 0.2f, 1f);
        starScaleYAnimator.setDuration(350);
        starScaleYAnimator.setStartDelay(250);
      //  starScaleYAnimator.setInterpolator(new OvershootInterpolator(4));

        ObjectAnimator starScaleXAnimator = ObjectAnimator.ofFloat(ivStar, ImageView.SCALE_X, 0.2f, 1f);
        starScaleXAnimator.setDuration(350);
        starScaleXAnimator.setStartDelay(250);*/
        //starScaleXAnimator.setInterpolator(new OvershootInterpolator(4));


        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("scaleX", .0f, 1.5f, 1, 1.5f, 0);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("scaleY", .0f, 1.5f, 1, 1.5f, 0);
        ObjectAnimator starScaleYAnimator = ObjectAnimator.ofPropertyValuesHolder(ivStar, pvhX, pvhY, PropertyValuesHolder.ofFloat(View.ALPHA.getName(), 0.2f, 1f, 1, 0.2f));

        starScaleYAnimator.setDuration(1500);
        //starScaleYAnimator.setRepeatMode(ValueAnimator.REVERSE);
        //starScaleYAnimator.setRepeatCount(1);

        ObjectAnimator outerCircleAnimator = ObjectAnimator.ofFloat(vCircle, CircleViewAnim.OUTER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
        outerCircleAnimator.setDuration(250);
        outerCircleAnimator.setInterpolator(new DecelerateInterpolator());

        ObjectAnimator innerCircleAnimator = ObjectAnimator.ofFloat(vCircle, CircleViewAnim.INNER_CIRCLE_RADIUS_PROGRESS, 0.1f, 1f);
        innerCircleAnimator.setDuration(200);
        innerCircleAnimator.setStartDelay(200);
        innerCircleAnimator.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator dotsAnimator = ObjectAnimator.ofFloat(vDotsView, DotsView.DOTS_PROGRESS, 0, 1f);
        dotsAnimator.setDuration(900);
        dotsAnimator.setStartDelay(50);
        dotsAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.playTogether(
                outerCircleAnimator,
                innerCircleAnimator,
                starScaleYAnimator,
                dotsAnimator
        );
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                //    super.onAnimationStart(animation);
                ivStar.setVisibility(View.VISIBLE);


            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                ivStar.setVisibility(View.GONE);

                new Handler().post(new Runnable() {
                    @Override
                    public void run() {

                        //     addToFav();
                    }
                });
            }

            @Override
            public void onAnimationCancel(Animator animation) {
               /* vCircle.setInnerCircleRadiusProgress(0);
                vCircle.setOuterCircleRadiusProgress(0);
                vDotsView.setCurrentProgress(0);
                ivStar.setScaleX(1);
                ivStar.setScaleY(1);
                ivStar.setAlpha(1f);*/
                ivStar.setVisibility(View.GONE);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {

                        //addToFav();
                    }
                });
            }
        });
        animatorSet.start();
    }

}
