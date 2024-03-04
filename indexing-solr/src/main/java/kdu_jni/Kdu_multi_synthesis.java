package kdu_jni;

public class Kdu_multi_synthesis {
  static {
    System.loadLibrary("kdu_jni");
    Native_init_class();
  }
  private static native void Native_init_class();
  protected long _native_ptr = 0;
  protected Kdu_multi_synthesis(long ptr) {
    _native_ptr = ptr;
  }
  public Kdu_multi_synthesis() {
      this(0);
  }
  public native boolean Exists() throws KduException;
  public native long Create(Kdu_codestream _codestream, Kdu_tile _tile, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags, int _buffer_rows, Kdu_sample_allocator _external_sample_allocator, Kdu_push_pull_params _extra_params, Kdu_membroker _membroker) throws KduException;
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags) throws KduException
  {
    Kdu_sample_allocator external_sample_allocator = null;
    Kdu_push_pull_params extra_params = null;
    Kdu_membroker membroker = null;
    return Create(_codestream,_tile,_env,_env_queue,_flags,(int) 1,external_sample_allocator,extra_params,membroker);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags, int _buffer_rows) throws KduException
  {
    Kdu_sample_allocator external_sample_allocator = null;
    Kdu_push_pull_params extra_params = null;
    Kdu_membroker membroker = null;
    return Create(_codestream,_tile,_env,_env_queue,_flags,_buffer_rows,external_sample_allocator,extra_params,membroker);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags, int _buffer_rows, Kdu_sample_allocator _external_sample_allocator) throws KduException
  {
    Kdu_push_pull_params extra_params = null;
    Kdu_membroker membroker = null;
    return Create(_codestream,_tile,_env,_env_queue,_flags,_buffer_rows,_external_sample_allocator,extra_params,membroker);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, Kdu_thread_env _env, Kdu_thread_queue _env_queue, int _flags, int _buffer_rows, Kdu_sample_allocator _external_sample_allocator, Kdu_push_pull_params _extra_params) throws KduException
  {
    Kdu_membroker membroker = null;
    return Create(_codestream,_tile,_env,_env_queue,_flags,_buffer_rows,_external_sample_allocator,_extra_params,membroker);
  }
  public native long Create(Kdu_codestream _codestream, Kdu_tile _tile, boolean _force_precise, boolean _skip_ycc, boolean _want_fastest, int _buffer_rows, Kdu_thread_env _env, Kdu_thread_queue _env_queue, boolean _multi_threaded_dwt) throws KduException;
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Create(_codestream,_tile,(boolean) false,(boolean) false,(boolean) false,(int) 1,env,env_queue,(boolean) false);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, boolean _force_precise) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Create(_codestream,_tile,_force_precise,(boolean) false,(boolean) false,(int) 1,env,env_queue,(boolean) false);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, boolean _force_precise, boolean _skip_ycc) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Create(_codestream,_tile,_force_precise,_skip_ycc,(boolean) false,(int) 1,env,env_queue,(boolean) false);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, boolean _force_precise, boolean _skip_ycc, boolean _want_fastest) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Create(_codestream,_tile,_force_precise,_skip_ycc,_want_fastest,(int) 1,env,env_queue,(boolean) false);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, boolean _force_precise, boolean _skip_ycc, boolean _want_fastest, int _buffer_rows) throws KduException
  {
    Kdu_thread_env env = null;
    Kdu_thread_queue env_queue = null;
    return Create(_codestream,_tile,_force_precise,_skip_ycc,_want_fastest,_buffer_rows,env,env_queue,(boolean) false);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, boolean _force_precise, boolean _skip_ycc, boolean _want_fastest, int _buffer_rows, Kdu_thread_env _env) throws KduException
  {
    Kdu_thread_queue env_queue = null;
    return Create(_codestream,_tile,_force_precise,_skip_ycc,_want_fastest,_buffer_rows,_env,env_queue,(boolean) false);
  }
  public long Create(Kdu_codestream _codestream, Kdu_tile _tile, boolean _force_precise, boolean _skip_ycc, boolean _want_fastest, int _buffer_rows, Kdu_thread_env _env, Kdu_thread_queue _env_queue) throws KduException
  {
    return Create(_codestream,_tile,_force_precise,_skip_ycc,_want_fastest,_buffer_rows,_env,_env_queue,(boolean) false);
  }
  public native boolean Start(Kdu_thread_env _env) throws KduException;
  public boolean Start() throws KduException
  {
    Kdu_thread_env env = null;
    return Start(env);
  }
  public native void Destroy(Kdu_thread_env _env) throws KduException;
  public void Destroy() throws KduException
  {
    Kdu_thread_env env = null;
    Destroy(env);
  }
  public native Kdu_coords Get_size(int _comp_idx) throws KduException;
  public native Kdu_line_buf Get_line(int _comp_idx, Kdu_thread_env _env) throws KduException;
  public Kdu_line_buf Get_line(int _comp_idx) throws KduException
  {
    Kdu_thread_env env = null;
    return Get_line(_comp_idx,env);
  }
  public native boolean Is_line_precise(int _comp_idx) throws KduException;
  public native boolean Is_line_absolute(int _comp_idx) throws KduException;
  public native byte Get_line_flags(int _comp_idx) throws KduException;
}
