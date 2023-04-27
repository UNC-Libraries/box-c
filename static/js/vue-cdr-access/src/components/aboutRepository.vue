<template>
    <header-small/>
    <div id="about-repo" class="content-wrap">
        <div class="contentarea">
            <h2>About this Repository</h2>
        </div>
        <p>The University of North Carolina at Chapel Hill Library’s Digital Collections Repository contains a portion of
            the Wilson Special Collection’s Library digital manuscript collections, university records, and publications.
            The Repository also contains select scholarly projects from university departments.</p>

        <h3>Related Wilson Special Collection Library Collections </h3>

        <p>Descriptions of collections (digital or physical) can be searched online at:</p>
        <ul>
            <li><a href="https://library.unc.edu/wilson/research/search/">https://library.unc.edu/wilson/research/search</a></li>
        </ul>

        <p>Scanned images of collection material, curated online exhibits, and virtual museum content:</p>
        <ul>
            <li><a href="https://library.unc.edu/find/digitalcollections/">https://library.unc.edu/find/digitalcollections/</a></li>
        </ul>

        <p>DocSouth Data:</p>
        <ul>
            <li><a href="https://docsouth.unc.edu/docsouthdata/">https://docsouth.unc.edu/docsouthdata</a></li>
        </ul>

        <p>Archived websites:</p>
        <ul>
            <li><a href="https://archive-it.org/collections/3491">https://archive-it.org/collections/3491</a></li>
        </ul>

        <h3>What are my responsibilities when using the digital files available through this website?</h3>

        <p>Many collection materials in the DCR are made available openly or by ONYEN authentication without requiring
            registration with the Wilson Special Collections Library. However, the materials are still governed by the
            Wilson Special Collections Library's Policies Governing Use. When you are use materials from the Repository,
            you should acknowledge and follow the following policies:</p>

        <h3>Policies Governing Use</h3>

        <p>Researchers agree to abide by restrictions placed on the library’s collections.</p>
        <p>Collection materials including unpublished documents may be subject to laws governing copyright
            (Title 17, U.S. Code). Digital files are made for private study and reference. Permission of the copyright
            holder may be required for uses exceeding "fair use." Researchers acknowledge that they are solely responsible
            for determining the copyright status of the materials they use.</p>
        <p>Manuscript collections and archival records that include twentieth and twenty-first century materials may
            contain sensitive or confidential information that is protected under federal or state right to privacy laws
            and regulations, the North Carolina Public Records Act (N.C.G.S. &sect; 132 1 et seq.), and Article 7 of the North
            Carolina State Personnel Act (Privacy of State Employee Personnel Records, N.C.G.S. &sect; 126-22 et seq.).
            Researchers are advised that the disclosure of certain information pertaining to identifiable living
            individuals without the consent of those individuals may have legal ramifications (e.g., a cause of action
            under common law for invasion of privacy may arise if facts concerning an individual's private life are
            published that would be deemed highly offensive to a reasonable person).</p>
        <p>The University Archives at the University of North Carolina at Chapel Hill operates in accordance with the
            North Carolina Public Records Act (N.C.G.S. &sect; 132 1 et seq.) providing unrestricted access to university
            records not covered by state and federal right to privacy acts.</p>
        <p>The University of North Carolina at Chapel Hill, and their libraries, assume no responsibility for
            infringement of literary property rights and copyright or for liability to any person for defamation or
            invasion of privacy that results from a researcher's use of collections.</p>
        <p>Researchers agree to indemnify and hold harmless the University of North Carolina at Chapel Hill, and their
            officers, employees, and agents from and against all suits, claims, actions, and expenses arising out of use
            of collections held by the libraries.</p>

        <h3>How We Preserve Digital Materials</h3>

        <p>Much of the material available in the Digital Collections Repository were acquired by the Library on various
            types of digital storage devices and via cloud storage. The storage devices range from floppy disks to DVDs
            to USB drives to Dropbox. In order to preserve the materials effectively we must carefully migrate the digital
            files to storage that is contemporary and optimized for long-term preservation. We employ best practices to
            analyze and migrate digital files from donated storage devices to the Digital Collections Repository
            for preservation and access.</p>
        <p>As a result of the legacy nature of some of the digital materials we receive, some digital files are difficult
            to access with contemporary software. This also means that some files may be inaccessible at present. As it
            becomes possible to do so, we will expand and improve access to legacy file formats based on emerging best
            practices and tool development.</p>
        <p>If you have questions about access to files, file formats, or storage devices used to transfer the materials
            to the Library, please contact us for more information.</p>

        <h3>How can I get more information or assistance?</h3>

        <p>You can visit our website: <a href="https://library.unc.edu/wilson/">https://library.unc.edu/wilson</a></p>
        <p>You can also contact us: <a href="mailto:wilsonlibrary@unc.edu">wilsonlibrary@unc.edu</a></p>
    </div>
</template>

<script>
import headerSmall from '@/components/header/headerSmall.vue';
import gaUtils from '../mixins/gaUtils';

export default {
    name: "aboutRepository",

    components: {headerSmall},

    mixins: [gaUtils],

    head() {
        return {
            title: 'About'
        }
    },

    mounted() {
        this.pageView('About');
    }
}
</script>

<style scoped lang="scss">
    #about-repo {
        width: 95%;

        h2 {
            font-size: 1.2rem;
            text-align: center;
        }

        h3 {
            font-size: 1.1rem;
        }

        p, a {
            font-size: .9rem;
        }

        ul {
            list-style-type: disc;
            margin-left: 15px;

            li {
                margin-left: 25px;
                text-indent: unset;
            }
        }
    }
</style>