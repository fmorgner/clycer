#ifndef FMO_CLYCER_HPP
#define FMO_CLYCER_HPP

#include <fstream>

namespace clycer
  {
  namespace impl
    {
    struct evaluate
      {
      template<typename ...IgnoredTypes> evaluate(IgnoredTypes const & ...) { }
      };
    }

  template<typename ...ProjectedTypes> void project(ProjectedTypes const & ...projected)
    {
    auto && projection = std::ofstream{"clycer.log"};
    (void)impl::evaluate{(projection << projected << '\n')...};
    }
  }

#endif
