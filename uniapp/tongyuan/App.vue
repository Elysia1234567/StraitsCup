<script>
export default {
  onLaunch() {
    console.log('App Launch')
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  },
}
</script>

<style lang="scss">
@import './styles/cyber.scss';
@import './styles/wot-cyber.scss';

page {
  background-color: $cyber-bg-void;
  color: $cyber-text;
  min-height: 100%;
}
</style>
