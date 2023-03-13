<template>
    <main>
        <div class="collection-links">
            <div class="collection-link-row">
                <a href="record/5bfe6a08-67d9-4d90-9e50-eeaf86aad37e">
                    <img :src="'/static/front/nc-collection.png'" alt="North Carolina Collection" aria-hidden="true">
                    <span>North Carolina Collection</span>
                </a>
                <a href="record/9ee8de0d-59ae-4c67-9686-78a79ebc93b1">
                    <img :src="'/static/front/university-archives.png'" alt="University Archives" aria-hidden="true">
                    <span>University Archives</span>
                </a>
            </div>
            <div class="collection-link-row">
                <a href="record/c59291a6-ad7a-4ad4-b89d-e2fe8acac744">
                    <img :src="'/static/front/southern-historical-collection.png'" alt="Southern Historical Collection" aria-hidden="true">
                    <span>Southern Historical Collection</span>
                </a>
                <a href="record/5e4b2719-bb71-45ec-be63-5d018b6f5aab">
                    <img :src="'/static/front/southern-folklife-collection.png'" alt="Southern Folklife Collection" aria-hidden="true">
                    <span>Southern Folklife Collection</span>
                </a>
            </div>
            <div class="collection-link-row">
                <a href="record/6f98967f-df96-452d-a202-0c99d1b7d951">
                    <img :src="'/static/front/rare-book-collection.png'" alt="Rare Books Collection" aria-hidden="true">
                    <span>Rare Books Collection</span>
                </a>
            </div>
        </div>
        <div class="info-row">
            <div class="info container">
                <h3>What's in the repository?</h3>
                <div class="info-icons">
                    <div><a href="search?format=Image"><i class="fas fa-image"></i>{{ collectionStats.formatCounts.image }} images</a></div>
                    <div><a href="search?format=Video"><i class="fas fa-video"></i>{{ collectionStats.formatCounts.video }} video files</a></div>
                    <div><a href="search?format=Audio"><i class="fas fa-music"></i>{{ collectionStats.formatCounts.audio }} audio files</a></div>
                    <div><a href="search?format=Text"><i class="fas fa-file-alt"></i>{{ collectionStats.formatCounts.text }} texts</a></div>
                </div>
                <p>Interested in seeing more?</p>
                <p>See <a href="https://library.unc.edu/find/digitalcollections/">more digital collections</a> or visit the <a href="https://library.unc.edu/wilson">Wilson Special Collections Library</a> website.</p>
            </div>
        </div>
    </main>
</template>

<script>
import get from "axios";

export default {
    name: "frontPage",

    data() {
        return {
            collectionStats: {
                formatCounts: {}
            }
        }
    },

    methods: {
        getCollectionStats() {
            get('/').then((response) => {
                this.collectionStats = response.data;
            }).catch(function (error) {
                console.log(error);
            });
        }
    },

    mounted() {
        this.getCollectionStats();
    }
}
</script>