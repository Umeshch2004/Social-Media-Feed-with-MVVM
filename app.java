
// --- Model Package ---

// Post.java
package com.example.socialfeed.model;

import java.util.Date;

public class Post {
    public final String id;
    public final User author;
    public final PostContent content;
    public final Date timestamp;

    public Post(String id, User author, PostContent content, Date timestamp) {
        this.id = id;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
    }
}

// User.java
package com.example.socialfeed.model;

public class User {
    public final String id;
    public final String username;
    public final String avatarURL;

    public User(String id, String username, String avatarURL) {
        this.id = id;
        this.username = username;
        this.avatarURL = avatarURL;
    }
}

// PostContent.java (Interface to support multiple content types)
package com.example.socialfeed.model;

public interface PostContent {
    int getViewType();
}

// TextContent.java
package com.example.socialfeed.model;

public class TextContent implements PostContent {
    public static final int VIEW_TYPE = 0;
    public final String text;

    public TextContent(String text) { this.text = text; }
    @Override public int getViewType() { return VIEW_TYPE; }
}

// ImageContent.java
package com.example.socialfeed.model;

public class ImageContent implements PostContent {
    public static final int VIEW_TYPE = 1;
    public final String imageURL;
    public final String caption;

    public ImageContent(String imageURL, String caption) {
        this.imageURL = imageURL;
        this.caption = caption;
    }
    @Override public int getViewType() { return VIEW_TYPE; }
}

// VideoContent.java
package com.example.socialfeed.model;

public class VideoContent implements PostContent {
    public static final int VIEW_TYPE = 2;
    public final String thumbnailURL;
    public final String videoURL;
    public final String caption;

    public VideoContent(String thumbnailURL, String videoURL, String caption) {
        this.thumbnailURL = thumbnailURL;
        this.videoURL = videoURL;
        this.caption = caption;
    }
    @Override public int getViewType() { return VIEW_TYPE; }
}


// --- Service Package ---

// FeedService.java
package com.example.socialfeed.service;

import com.example.socialfeed.model.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface FeedCallback {
        void onSuccess(List<Post> posts);
        void onError(Exception e);
    }

    public void fetchInitialPosts(FeedCallback callback) {
        fetchPosts(callback, 1500);
    }

    public void fetchMorePosts(FeedCallback callback) {
        fetchPosts(callback, 2000);
    }

    private void fetchPosts(FeedCallback callback, int delay) {
         executor.execute(() -> {
            try {
                Thread.sleep(delay);
                callback.onSuccess(generateMockPosts(20));
            } catch (InterruptedException e) {
                callback.onError(e);
            }
        });
    }

    private List<Post> generateMockPosts(int count) {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User("user_" + i, "User " + i, "https://placehold.co/100x100/EFEFEF/333333?text=U" + i);
            PostContent content;
            switch ((int) (Math.random() * 3)) {
                case 1:
                    content = new ImageContent("https://placehold.co/600x400/CCCCCC/333333?text=Image", "An interesting image caption. #scenery");
                    break;
                case 2:
                    content = new VideoContent("https://placehold.co/600x400/AAAAAA/FFFFFF?text=Video", "about:blank", "A cool video preview. #fun");
                    break;
                default:
                    content = new TextContent("This is a sample text post. It can have a variable amount of text, which the UI needs to handle gracefully. Post number " + i + ".");
                    break;
            }
            posts.add(new Post(UUID.randomUUID().toString(), user, content, new Date(System.currentTimeMillis() - (long)i * 3600 * 1000)));
        }
        return posts;
    }
}


// --- ViewModel Package ---

// FeedViewModel.java
package com.example.socialfeed.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.socialfeed.model.Post;
import com.example.socialfeed.service.FeedService;
import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends ViewModel {
    private final FeedService feedService = new FeedService();

    private final MutableLiveData<List<Post>> _posts = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Post>> posts = _posts;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Exception> _error = new MutableLiveData<>(null);
    public final LiveData<Exception> error = _error;

    private boolean isMoreDataAvailable = true;

    public void loadInitialPosts() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) return;
        _isLoading.setValue(true);

        feedService.fetchInitialPosts(new FeedService.FeedCallback() {
            @Override
            public void onSuccess(List<Post> newPosts) {
                _posts.setValue(newPosts);
                isMoreDataAvailable = !newPosts.isEmpty();
                _isLoading.setValue(false);
            }
            @Override
            public void onError(Exception e) {
                _error.setValue(e);
                _isLoading.setValue(false);
            }
        });
    }

    public void loadMorePosts() {
        if (Boolean.TRUE.equals(_isLoading.getValue()) || !isMoreDataAvailable) return;
        _isLoading.setValue(true);

        feedService.fetchMorePosts(new FeedService.FeedCallback() {
            @Override
            public void onSuccess(List<Post> newPosts) {
                if (newPosts.isEmpty()) {
                    isMoreDataAvailable = false;
                } else {
                    List<Post> currentPosts = new ArrayList<>(_posts.getValue());
                    currentPosts.addAll(newPosts);
                    _posts.setValue(currentPosts);
                }
                _isLoading.setValue(false);
            }
            @Override
            public void onError(Exception e) {
                _error.setValue(e);
                _isLoading.setValue(false);
            }
        });
    }
}


// --- View Package (Activity, Adapter, ViewHolders) ---

// FeedActivity.java
package com.example.socialfeed.view;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.socialfeed.R;
import com.example.socialfeed.viewmodel.FeedViewModel;
import com.google.android.material.snackbar.Snackbar;

public class FeedActivity extends AppCompatActivity {
    private FeedViewModel viewModel;
    private FeedAdapter feedAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar bottomProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        setupUI();
        observeViewModel();

        if (viewModel.posts.getValue() == null || viewModel.posts.getValue().isEmpty()) {
            viewModel.loadInitialPosts();
        }
    }

    private void setupUI() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        bottomProgressBar = findViewById(R.id.bottom_progress_bar);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_feed);
        
        feedAdapter = new FeedAdapter();
        recyclerView.setAdapter(feedAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.loadInitialPosts());

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == feedAdapter.getItemCount() - 1) {
                    viewModel.loadMorePosts();
                }
            }
        });
    }

    private void observeViewModel() {
        viewModel.posts.observe(this, posts -> feedAdapter.submitList(posts));

        viewModel.isLoading.observe(this, isLoading -> {
            if (!isLoading) {
                swipeRefreshLayout.setRefreshing(false);
            }
            boolean showBottomLoader = isLoading && feedAdapter.getItemCount() > 0;
            bottomProgressBar.setVisibility(showBottomLoader ? View.VISIBLE : View.GONE);
        });

        viewModel.error.observe(this, error -> {
            if (error != null) {
                Snackbar.make(findViewById(android.R.id.content), "An error occurred", Snackbar.LENGTH_LONG).show();
            }
        });
    }
}

// FeedAdapter.java
package com.example.socialfeed.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.socialfeed.R;
import com.example.socialfeed.model.*;
import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<BasePostViewHolder> {
    private List<Post> posts = new ArrayList<>();

    @Override
    public int getItemViewType(int position) {
        return posts.get(position).content.getViewType();
    }

    @NonNull
    @Override
    public BasePostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ImageContent.VIEW_TYPE:
                return new ImagePostViewHolder(inflater.inflate(R.layout.item_post_image, parent, false));
            case VideoContent.VIEW_TYPE:
                return new VideoPostViewHolder(inflater.inflate(R.layout.item_post_video, parent, false));
            default: // TextContent.VIEW_TYPE
                return new TextPostViewHolder(inflater.inflate(R.layout.item_post_text, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BasePostViewHolder holder, int position) {
        holder.bind(posts.get(position));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void submitList(List<Post> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged(); // For simplicity. Use DiffUtil for better performance.
    }
}

// BasePostViewHolder.java
package com.example.socialfeed.view;

import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.socialfeed.R;
import com.example.socialfeed.model.Post;

public abstract class BasePostViewHolder extends RecyclerView.ViewHolder {
    protected final ImageView avatarImageView;
    protected final TextView usernameTextView;
    protected final TextView timestampTextView;

    public BasePostViewHolder(View itemView) {
        super(itemView);
        avatarImageView = itemView.findViewById(R.id.image_view_avatar);
        usernameTextView = itemView.findViewById(R.id.text_view_username);
        timestampTextView = itemView.findViewById(R.id.text_view_timestamp);
    }

    public void bind(Post post) {
        usernameTextView.setText(post.author.username);
        String timeAgo = DateUtils.getRelativeTimeSpanString(post.timestamp.getTime()).toString();
        timestampTextView.setText(timeAgo);

        Glide.with(itemView.getContext())
             .load(post.author.avatarURL)
             .circleCrop()
             .into(avatarImageView);
    }
}

// TextPostViewHolder.java
package com.example.socialfeed.view;

import android.view.View;
import android.widget.TextView;
import com.example.socialfeed.R;
import com.example.socialfeed.model.Post;
import com.example.socialfeed.model.TextContent;

public class TextPostViewHolder extends BasePostViewHolder {
    private final TextView contentTextView;

    public TextPostViewHolder(View itemView) {
        super(itemView);
        contentTextView = itemView.findViewById(R.id.text_view_content);
    }

    @Override
    public void bind(Post post) {
        super.bind(post);
        if (post.content instanceof TextContent) {
            contentTextView.setText(((TextContent) post.content).text);
        }
    }
}

// MediaPostViewHolder.java (A new base class for image and video)
package com.example.socialfeed.view;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.socialfeed.R;

public abstract class MediaPostViewHolder extends BasePostViewHolder {
    protected final ImageView postImageView;
    protected final TextView captionTextView;

    public MediaPostViewHolder(View itemView) {
        super(itemView);
        postImageView = itemView.findViewById(R.id.image_view_post);
        captionTextView = itemView.findViewById(R.id.text_view_caption);
    }

    protected void bindMedia(String imageUrl, String caption) {
        captionTextView.setText(caption);
        captionTextView.setVisibility(caption != null && !caption.isEmpty() ? View.VISIBLE : View.GONE);

        Glide.with(itemView.getContext())
             .load(imageUrl)
             .into(postImageView);
    }
}


// ImagePostViewHolder.java
package com.example.socialfeed.view;

import android.view.View;
import com.example.socialfeed.model.ImageContent;
import com.example.socialfeed.model.Post;

public class ImagePostViewHolder extends MediaPostViewHolder {
    public ImagePostViewHolder(View itemView) { super(itemView); }

    @Override
    public void bind(Post post) {
        super.bind(post);
        if (post.content instanceof ImageContent) {
            ImageContent content = (ImageContent) post.content;
            bindMedia(content.imageURL, content.caption);
        }
    }
}

// VideoPostViewHolder.java
package com.example.socialfeed.view;

import android.view.View;
import com.example.socialfeed.model.Post;
import com.example.socialfeed.model.VideoContent;

public class VideoPostViewHolder extends MediaPostViewHolder {
    public VideoPostViewHolder(View itemView) { super(itemView); }

    @Override
    public void bind(Post post) {
        super.bind(post);
        if (post.content instanceof VideoContent) {
            VideoContent content = (VideoContent) post.content;
            bindMedia(content.thumbnailURL, content.caption);
        }
    }
}
```xml
<!-- res/layout/activity_feed.xml -->
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)"
    xmlns:tools="[http://schemas.android.com/tools](http://schemas.android.com/tools)"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".view.FeedActivity">

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipe_refresh_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_view_feed"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clipToPadding="false"
            android:paddingBottom="8dp"
            tools:listitem="@layout/item_post_text" />

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <ProgressBar
        android:id="@+id/bottom_progress_bar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        tools:visibility="visible" />

</androidx.constraintlayout.widget.ConstraintLayout>

<!-- res/layout/item_post_base.xml (A reusable header for all post types) -->
<merge xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)"
    xmlns:tools="[http://schemas.android.com/tools](http://schemas.android.com/tools)">

    <ImageView
        android:id="@+id/image_view_avatar"
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:layout_marginStart="16dp"
        android:layout_marginTop="16dp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        tools:src="@tools:sample/avatars" />

    <TextView
        android:id="@+id/text_view_username"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:layout_marginEnd="16dp"
        android:textColor="@android:color/black"
        android:textStyle="bold"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@+id/image_view_avatar"
        app:layout_constraintTop_toTopOf="@+id/image_view_avatar"
        tools:text="Username" />

    <TextView
        android:id="@+id/text_view_timestamp"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintEnd_toEndOf="@+id/text_view_username"
        app:layout_constraintStart_toStartOf="@+id/text_view_username"
        app:layout_constraintTop_toBottomOf="@+id/text_view_username"
        tools:text="1h ago" />
</merge>

<!-- res/layout/item_post_text.xml -->
<androidx.cardview.widget.CardView
    xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)"
    xmlns:tools="[http://schemas.android.com/tools](http://schemas.android.com/tools)"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="8dp"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="8dp">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingBottom="16dp">

        <include layout="@layout/item_post_base" />

        <TextView
            android:id="@+id/text_view_content"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:lineSpacingExtra="4dp"
            android:textColor="@android:color/black"
            app:layout_constraintEnd_toEndOf="@+id/text_view_username"
            app:layout_constraintStart_toStartOf="@+id/image_view_avatar"
            app:layout_constraintTop_toBottomOf="@+id/image_view_avatar"
            tools:text="This is the content of the post. It can be multiple lines long." />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>

<!-- res/layout/item_post_image.xml -->
<androidx.cardview.widget.CardView
    xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)"
    xmlns:tools="[http://schemas.android.com/tools](http://schemas.android.com/tools)"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="8dp"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="8dp">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingBottom="16dp">

        <include layout="@layout/item_post_base" />

        <ImageView
            android:id="@+id/image_view_post"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_marginTop="12dp"
            android:scaleType="centerCrop"
            app:layout_constraintDimensionRatio="16:9"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/image_view_avatar"
            tools:src="@tools:sample/backgrounds/scenic" />

        <TextView
            android:id="@+id/text_view_caption"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:layout_marginHorizontal="16dp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/image_view_post"
            tools:text="This is a caption for the image." />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>

<!-- res/layout/item_post_video.xml -->
<androidx.cardview.widget.CardView
    xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)"
    xmlns:app="[http://schemas.android.com/apk/res-auto](http://schemas.android.com/apk/res-auto)"
    xmlns:tools="[http://schemas.android.com/tools](http://schemas.android.com/tools)"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="8dp"
    android:layout_marginVertical="4dp"
    app:cardCornerRadius="8dp">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingBottom="16dp">

        <include layout="@layout/item_post_base" />

        <ImageView
            android:id="@+id/image_view_post"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_marginTop="12dp"
            android:scaleType="centerCrop"
            app:layout_constraintDimensionRatio="16:9"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/image_view_avatar"
            tools:src="@tools:sample/backgrounds/scenic" />

        <ImageView
            android:id="@+id/image_view_play_icon"
            android:layout_width="60dp"
            android:layout_height="60dp"
            android:src="@android:drawable/ic_media_play"
            app:layout_constraintBottom_toBottomOf="@+id/image_view_post"
            app:layout_constraintEnd_toEndOf="@+id/image_view_post"
            app:layout_constraintStart_toStartOf="@+id/image_view_post"
            app:layout_constraintTop_toTopOf="@+id/image_view_post"
            app:tint="@android:color/white" />

        <TextView
            android:id="@+id/text_view_caption"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:layout_marginHorizontal="16dp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/image_view_post"
            tools:text="This is a caption for the video." />

    </androidx.constraintlayout.widget.ConstraintLayout>
</androidx.cardview.widget.CardView>
